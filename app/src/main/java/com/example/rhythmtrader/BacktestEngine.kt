package com.example.rhythmtrader

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object BacktestEngine {

    data class Candle(
        val time: Long,
        val open: Double,
        val high: Double,
        val low: Double,
        val close: Double,
        val volume: Double
    )

    data class SweepCell(
        val thresholdPct: Double,
        val slBps: Double,
        val tpBps: Double,
        val trainNetBps: Double,
        val trainSharpe: Double,
        val oosNetBps: Double,
        val oosSharpe: Double,
        val totalTrades: Int,
        val winRatePct: Double,
        val maxDdBps: Double,
        val verdict: String, // DATA VALIDATED, TRADE-OFF, NO EDGE
        val strategyMode: Int = 0
    )

    data class SweepReport(
        val symbol: String,
        val strategyName: String,
        val candlesCount: Int,
        val bestCell: SweepCell,
        val topCells: List<SweepCell>,
        val isPlateau: Boolean,
        val flashCrash5PctImpactUsdt: Double,
        val cascade12PctImpactUsdt: Double,
        val bestStrategyMode: Int = 0
    )

    private val httpClient = OkHttpClient()

    fun runFullSweep(symbol: String, strategyMode: Int, currentBalance: Double): SweepReport {
        val candles = fetchHistoricalCandles(symbol)
        return evaluateCandlesForStrategy(symbol, candles, strategyMode)
    }

    /**
     * Полная Авто-Адаптация (Self-Healing):
     * Скачивает 500 свечей один раз и проверяет все 5 постулатов (5 x 20 = 100 конфигураций),
     * выбирая стратегию и порог с лучшим Out-of-Sample и устойчивым плато.
     */
    fun runMultiStrategyAutoAdapt(symbol: String, currentBalance: Double): SweepReport {
        val candles = fetchHistoricalCandles(symbol)
        if (candles.size < 60) {
            return evaluateCandlesForStrategy(symbol, candles, RadarService.botStrategyMode)
        }

        val reports = (0..4).map { mode ->
            evaluateCandlesForStrategy(symbol, candles, mode)
        }

        val bestReport = reports.sortedWith(
            compareByDescending<SweepReport> {
                when (it.bestCell.verdict) {
                    "DATA VALIDATED" -> 2
                    "TRADE-OFF" -> 1
                    else -> 0
                }
            }.thenByDescending { if (it.isPlateau) 1 else 0 }
             .thenByDescending { it.bestCell.oosNetBps * 1.2 + it.bestCell.trainNetBps * 0.8 }
        ).first()

        return bestReport
    }

    private fun evaluateCandlesForStrategy(symbol: String, candles: List<Candle>, strategyMode: Int): SweepReport {
        val stratName = QuantEngine.getStrategyShortTitle(strategyMode)

        if (candles.size < 60) {
            val emptyCell = SweepCell(0.35, 60.0, 120.0, 0.0, 0.0, 0.0, 0.0, 0, 0.0, 0.0, "NO EDGE", strategyMode)
            return SweepReport(symbol, stratName, candles.size, emptyCell, listOf(emptyCell), false, -20.0, -20.0, strategyMode)
        }

        val splitIdx = candles.size / 2
        val trainCandles = candles.subList(0, splitIdx)
        val oosCandles = candles.subList(splitIdx, candles.size)

        val thresholds = listOf(0.15, 0.25, 0.35, 0.45, 0.60)
        val slMultipliers = listOf(1.0, 1.3, 1.6, 2.0)

        val allCells = ArrayList<SweepCell>()

        for (thresh in thresholds) {
            for (mult in slMultipliers) {
                val strategyBracketMultiplier = if (strategyMode == 3) 1.5 else 1.0
                val slBps = StrategyCore.stopLossBps(thresh, mult * strategyBracketMultiplier)
                val tpBps = StrategyCore.takeProfitBps(slBps)

                val trainRes = simulateSegment(trainCandles, strategyMode, thresh, slBps, tpBps)
                val oosRes = simulateSegment(oosCandles, strategyMode, thresh, slBps, tpBps)

                val verdict = when {
                    trainRes.netBps > 15.0 && oosRes.netBps > 15.0 && oosRes.sharpe >= 0.4 -> "DATA VALIDATED"
                    (trainRes.netBps + oosRes.netBps) > 10.0 && (trainRes.netBps > 0 || oosRes.netBps > 0) -> "TRADE-OFF"
                    else -> "NO EDGE"
                }

                val totalTrades = trainRes.trades + oosRes.trades
                val totalWins = trainRes.wins + oosRes.wins
                val winRate = if (totalTrades > 0) (totalWins.toDouble() / totalTrades) * 100.0 else 0.0

                allCells.add(
                    SweepCell(
                        thresholdPct = thresh,
                        slBps = slBps,
                        tpBps = tpBps,
                        trainNetBps = trainRes.netBps,
                        trainSharpe = trainRes.sharpe,
                        oosNetBps = oosRes.netBps,
                        oosSharpe = oosRes.sharpe,
                        totalTrades = totalTrades,
                        winRatePct = winRate,
                        maxDdBps = max(trainRes.maxDdBps, oosRes.maxDdBps),
                        verdict = verdict,
                        strategyMode = strategyMode
                    )
                )
            }
        }

        val sorted = allCells.sortedWith(
            compareByDescending<SweepCell> {
                when (it.verdict) {
                    "DATA VALIDATED" -> 2
                    "TRADE-OFF" -> 1
                    else -> 0
                }
            }.thenByDescending { it.oosNetBps * 1.2 + it.trainNetBps * 0.8 }
        )

        val best = sorted.first()

        val neighbors = allCells.filter {
            abs(it.thresholdPct - best.thresholdPct) <= 0.11 && abs(it.slBps - best.slBps) <= 45.0
        }
        val positiveNeighbors = neighbors.count { (it.trainNetBps + it.oosNetBps) > 0 }
        val isPlateau = neighbors.isNotEmpty() && (positiveNeighbors.toDouble() / neighbors.size) >= 0.60

        val pos = RadarService.openPositions[symbol]
        val activeMargin = pos?.marginUsdt ?: 100.0
        val activeLev = pos?.leverage ?: 5
        val bracketCutPct = best.slBps / 10000.0
        val maxBracketLossUsdt = -(activeMargin * activeLev * (bracketCutPct + 0.0011))
        val unhedged12PctLoss = -(activeMargin * activeLev * 0.12)

        return SweepReport(
            symbol = symbol,
            strategyName = stratName,
            candlesCount = candles.size,
            bestCell = best,
            topCells = sorted.take(5),
            isPlateau = isPlateau,
            flashCrash5PctImpactUsdt = maxBracketLossUsdt,
            cascade12PctImpactUsdt = max(maxBracketLossUsdt * 1.25, unhedged12PctLoss),
            bestStrategyMode = strategyMode
        )
    }

    private data class SegmentResult(val netBps: Double, val sharpe: Double, val trades: Int, val wins: Int, val maxDdBps: Double)

    private fun simulateSegment(
        candles: List<Candle>,
        strategyMode: Int,
        thresholdPct: Double,
        slBps: Double,
        tpBps: Double
    ): SegmentResult {
        if (candles.size < 25) return SegmentResult(0.0, 0.0, 0, 0, 0.0)

        val returnsList = ArrayList<Double>()
        var cumulativeBps = 0.0
        var peakBps = 0.0
        var maxDdBps = 0.0
        var wins = 0
        val recentTradeExitIndices = ArrayList<Int>()

        var i = 20
        while (i < candles.size - 1) {
            recentTradeExitIndices.removeAll { it < i - 12 }
            val window = candles.subList(i - 20, i)
            var sumPV = 0.0
            var sumV = 0.0
            var buyV = 0.0
            var sellV = 0.0
            var winHigh = Double.MIN_VALUE
            var winLow = Double.MAX_VALUE

            for (c in window) {
                val typ = (c.high + c.low + c.close) / 3.0
                sumPV += typ * c.volume
                sumV += c.volume
                if (c.close >= c.open) buyV += c.volume else sellV += c.volume
                if (c.high > winHigh) winHigh = c.high
                if (c.low < winLow) winLow = c.low
            }

            val cur = candles[i]
            val price = cur.close
            val vwap = if (sumV > 0) sumPV / sumV else price
            val ofi = (buyV - sellV) / max(buyV + sellV, 1.0)

            var sumSq = 0.0
            for (c in window) {
                val d = c.close - vwap
                sumSq += d * d
            }
            val std = max(sqrt(sumSq / window.size), price * 0.0005)
            val zScore = (price - vwap) / std
            val moveFromVwapPct = ((price - vwap) / vwap) * 100.0
            val recentWindow = window.takeLast(5)
            val recentBuy = recentWindow.filter { it.close >= it.open }.sumOf { it.volume }
            val recentSell = recentWindow.filter { it.close < it.open }.sumOf { it.volume }
            val avgVolume = window.map { it.volume }.average().coerceAtLeast(1.0)
            val returnsVol = window.zipWithNext { a, b -> (b.close - a.close) / a.close }
                .map { it * it }.average().let { sqrt(it.coerceAtLeast(0.0)) }
            val volBps = (returnsVol * 10000.0 * sqrt(120.0)).coerceIn(15.0, 260.0)
            val sextile = when {
                volBps < 40.0 -> 1
                volBps < 60.0 -> 2
                volBps < 82.0 -> 3
                volBps < 108.0 -> 4
                volBps < 148.0 -> 5
                else -> 6
            }
            val signal = StrategyCore.evaluate(
                StrategyCore.Inputs(
                    mode = strategyMode,
                    zScore = zScore,
                    ofi = ofi,
                    moveFromVwapPct = moveFromVwapPct,
                    thresholdPct = thresholdPct,
                    price = price,
                    windowHigh = winHigh,
                    windowLow = winLow,
                    recentBuyVolume = recentBuy,
                    recentSellVolume = recentSell,
                    volatilitySextile = sextile,
                    recentTradesLastHour = recentTradeExitIndices.size,
                    volumeRatio = cur.volume / avgVolume,
                    whaleFlow = cur.volume >= avgVolume * 1.8
                )
            )
            val backtestLeverage = when (sextile) {
                1 -> 8
                2 -> 6
                3 -> 5
                4 -> 4
                5 -> 3
                else -> 2
            }
            val sideAlignedMove = if (signal.action > 0) moveFromVwapPct else -moveFromVwapPct
            val riskBlocked = signal.action != 0 &&
                StrategyCore.h2RiskScore(sextile, backtestLeverage, sideAlignedMove) >= 0.72
            val dir = if (riskBlocked) 0 else signal.action

            if (dir != 0) {
                val entry = price
                val tpPrice = if (dir > 0) entry * (1.0 + tpBps / 10000.0) else entry * (1.0 - tpBps / 10000.0)
                var slPrice = if (dir > 0) entry * (1.0 - slBps / 10000.0) else entry * (1.0 + slBps / 10000.0)
                var mfeLocked = false
                var exitBps = 0.0
                var closed = false

                var j = i + 1
                val maxHold = min(i + 24, candles.size - 1)
                while (j <= maxHold) {
                    val bar = candles[j]
                    val favMoveBps = if (dir > 0) ((bar.high - entry) / entry) * 10000.0 else ((entry - bar.low) / entry) * 10000.0

                    // Resolve the stop/target using levels known before this candle's close.
                    // If both are touched in one candle, the conservative stop-first rule applies.
                    val hitSl = if (dir > 0) bar.low <= slPrice else bar.high >= slPrice
                    val hitTp = if (dir > 0) bar.high >= tpPrice else bar.low <= tpPrice

                    if (hitSl) {
                        val rawBps = if (dir > 0) ((slPrice - entry) / entry) * 10000.0 else ((entry - slPrice) / entry) * 10000.0
                        exitBps = rawBps - QuantEngine.ROUNDTRIP_FEE_BPS
                        closed = true
                        i = j
                        break
                    } else if (hitTp) {
                        exitBps = tpBps - QuantEngine.ROUNDTRIP_FEE_BPS
                        closed = true
                        i = j
                        break
                    }
                    // Activate a protective stop only for subsequent candles; avoid same-bar lookahead.
                    if (!mfeLocked && favMoveBps >= 45.0) {
                        mfeLocked = true
                        slPrice = if (dir > 0) entry * 1.0015 else entry * 0.9985
                    }
                    j++
                }

                if (!closed) {
                    val lastClose = candles[maxHold].close
                    val rawBps = if (dir > 0) ((lastClose - entry) / entry) * 10000.0 else ((entry - lastClose) / entry) * 10000.0
                    exitBps = rawBps - QuantEngine.ROUNDTRIP_FEE_BPS
                    i = maxHold
                }

                recentTradeExitIndices.add(i)
                returnsList.add(exitBps)
                if (exitBps > 0) wins++
                cumulativeBps += exitBps
                if (cumulativeBps > peakBps) peakBps = cumulativeBps
                val dd = peakBps - cumulativeBps
                if (dd > maxDdBps) maxDdBps = dd
            }
            i++
        }

        if (returnsList.isEmpty()) return SegmentResult(0.0, 0.0, 0, 0, 0.0)
        val mean = returnsList.average()
        var sq = 0.0
        for (r in returnsList) {
            val d = r - mean
            sq += d * d
        }
        val std = max(sqrt(sq / returnsList.size), 1.0)
        val sharpe = (mean / std) * sqrt(returnsList.size.toDouble())
        return SegmentResult(cumulativeBps, sharpe, returnsList.size, wins, maxDdBps)
    }

    private fun fetchHistoricalCandles(symbol: String): List<Candle> {
        val list = ArrayList<Candle>()
        try {
            val url = "https://open-api.bingx.com/openApi/swap/v3/quote/klines?symbol=$symbol&interval=5m&limit=500"
            val req = Request.Builder().url(url).build()
            httpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: return list
                val json = JSONObject(body)
                val dataArr = json.optJSONArray("data") ?: return list
                for (i in 0 until dataArr.length()) {
                    val obj = dataArr.getJSONObject(i)
                    list.add(
                        Candle(
                            time = obj.optLong("time", 0L),
                            open = obj.optString("open", "0").toDouble(),
                            high = obj.optString("high", "0").toDouble(),
                            low = obj.optString("low", "0").toDouble(),
                            close = obj.optString("close", "0").toDouble(),
                            volume = obj.optString("volume", "0").toDouble()
                        )
                    )
                }
            }
            list.sortBy { it.time }
        } catch (_: Exception) {}
        return list
    }
}
