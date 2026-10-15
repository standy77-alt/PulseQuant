package com.example.rhythmtrader

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

object QuantEngine {

    data class MarketSnapshot(
        val symbol: String,
        val timestamp: Long,
        val price: Double,
        val vwap: Double,
        val fairLow: Double,          // ANALYST Bot: Нижняя граница справедливой цены
        val fairHigh: Double,         // ANALYST Bot: Верхняя граница справедливой цены
        val valuationTag: String,     // CHEAP / FAIR / EXPENSIVE (Rule 2 Jane Street)
        val zScore: Double,
        val volBpsPerHour: Double,
        val volSextile: Int,
        val recommendedLeverage: Int,
        val ofi: Double,
        val moveFromVwapPct: Double,
        val regime: String
    )

    data class QuantDecision(
        val action: Int,
        val strategyName: String,
        val leverage: Int,
        val marginUsdt: Double,
        val stopLossBps: Double,
        val takeProfitBps: Double,
        val h2RiskScore: Double,
        val expectedEdgeBps: Double,
        val reason: String
    )

    // SKEPTIC Bot: запись об убитой (отклоненной) сделке для проверки, сэкономил ли Скептик деньги
    data class KilledShadowTrade(
        val symbol: String,
        val wouldBeLong: Boolean,
        val priceAtKill: Double,
        val killTimeMs: Long,
        val reason: String,
        var evaluated: Boolean = false
    )

    private data class TickRecord(val price: Double, val volUsdt: Double, val isBuy: Boolean, val ts: Long)

    private val tickWindows = ConcurrentHashMap<String, CopyOnWriteArrayList<TickRecord>>()
    val latestSnapshots = ConcurrentHashMap<String, MarketSnapshot>()
    val latestDecisions = ConcurrentHashMap<String, QuantDecision>()
    private val shadowKills = CopyOnWriteArrayList<KilledShadowTrade>()

    val closedMfeBpsList = CopyOnWriteArrayList<Double>()
    val closedRealizedBpsList = CopyOnWriteArrayList<Double>()
    var h2FlaggedOrdersCount = 0
    var chaseBlockedCount = 0
    var skepticSavedReadsCount = 0
    var skepticSavedUsdtEstimate = 0.0
    var killSwitchEngaged = false

    const val CANON_FEE_BPS_PER_SIDE = 5.5
    const val ROUNDTRIP_FEE_BPS = 11.0

    fun getStrategyShortTitle(mode: Int): String {
        return when (mode) {
            0 -> "🛡️ 1. Patient Mean-Reversion"
            1 -> "⚡ 2. Anti-Chase Momentum"
            2 -> "💥 3. Squeeze / Stop-Hunt"
            3 -> "🧘 4. Core / Patient Sniper"
            else -> "🔬 5. Microstructure Whale-Flow"
        }
    }

    // JOURNAL Bot (Rule 7): проверка деградации стратегии по последним сделкам
    fun isStrategyDecaying(): Boolean {
        if (closedRealizedBpsList.size < 5) return false
        val recent5 = closedRealizedBpsList.takeLast(5)
        return recent5.average() < -8.0
    }

    fun recordTickAndEvaluate(
        symbol: String,
        price: Double,
        volUsdt: Double,
        isBuy: Boolean,
        nowMs: Long,
        strategyMode: Int,
        thresholdPct: Double,
        whaleThresholdUsdt: Double,
        currentInventoryCount: Int,
        hasPositionOnSymbol: Boolean,
        recentTradesLastHour: Int
    ): QuantDecision {
        val window = tickWindows.getOrPut(symbol) { CopyOnWriteArrayList() }
        window.add(TickRecord(price, volUsdt, isBuy, nowMs))
        while (window.size > 120) window.removeAt(0)

        // SKEPTIC & JOURNAL Audit: проверяем, что случилось с убитыми сделками спустя 3 минуты
        for (sk in shadowKills) {
            if (!sk.evaluated && sk.symbol == symbol && (nowMs - sk.killTimeMs) >= 180_000L) {
                sk.evaluated = true
                val diff = if (sk.wouldBeLong) (price - sk.priceAtKill) / sk.priceAtKill else (sk.priceAtKill - price) / sk.priceAtKill
                if (diff < 0) {
                    skepticSavedReadsCount++
                    skepticSavedUsdtEstimate += abs(diff) * 500.0
                }
            }
        }
        shadowKills.removeAll { it.evaluated }

        var sumPV = 0.0
        var sumV = 0.0
        var buyV = 0.0
        var sellV = 0.0
        var windowHigh = Double.MIN_VALUE
        var windowLow = Double.MAX_VALUE

        for (i in window.indices) {
            val t = window[i]
            sumPV += t.price * t.volUsdt
            sumV += t.volUsdt
            if (t.isBuy) buyV += t.volUsdt else sellV += t.volUsdt
            if (i < window.size - 3) {
                if (t.price > windowHigh) windowHigh = t.price
                if (t.price < windowLow) windowLow = t.price
            }
        }

        val vwap = if (sumV > 0) sumPV / sumV else price
        val totalV = max(buyV + sellV, 1.0)
        val ofi = (buyV - sellV) / totalV

        var sumSqDiff = 0.0
        var sumRetSq = 0.0
        for (i in window.indices) {
            val d = window[i].price - vwap
            sumSqDiff += d * d
            if (i > 0) {
                val r = (window[i].price - window[i - 1].price) / window[i - 1].price
                sumRetSq += r * r
            }
        }

        val n = max(window.size, 1)
        val stdPrice = max(sqrt(sumSqDiff / n), price * 0.0002)
        val zScore = ((price - vwap) / stdPrice).coerceIn(-4.5, 4.5)

        // ANALYST Bot (Rule 2): вычисляем справедливый коридор стоимости [VWAP - 1.2σ .. VWAP + 1.2σ]
        val fairLow = vwap - stdPrice * 1.2
        val fairHigh = vwap + stdPrice * 1.2
        val valuationTag = when {
            price > fairHigh -> "EXPENSIVE (Дорого)"
            price < fairLow -> "CHEAP (Дешево)"
            else -> "FAIR VALUE (Норма)"
        }

        val volBpsPerHour = (sqrt(sumRetSq / n) * 10000.0 * sqrt(120.0)).coerceIn(15.0, 260.0)
        val sextile = when {
            volBpsPerHour < 40.0 -> 1
            volBpsPerHour < 60.0 -> 2
            volBpsPerHour < 82.0 -> 3
            volBpsPerHour < 108.0 -> 4
            volBpsPerHour < 148.0 -> 5
            else -> 6
        }

        val scaledLeverage = when (sextile) {
            1 -> 8
            2 -> 6
            3 -> 5
            4 -> 4
            5 -> 3
            else -> 2
        }

        val moveFromVwapPct = ((price - vwap) / vwap) * 100.0
        val regime = when {
            abs(moveFromVwapPct) >= 0.75 -> "CHASE-ZONE (>0.75%)"
            sextile >= 5 -> "WILD-VOL (S$sextile)"
            abs(zScore) > 1.7 -> "DISLOCATED"
            else -> "BALANCED (S$sextile)"
        }

        val snap = MarketSnapshot(
            symbol = symbol,
            timestamp = nowMs,
            price = price,
            vwap = vwap,
            fairLow = fairLow,
            fairHigh = fairHigh,
            valuationTag = valuationTag,
            zScore = zScore,
            volBpsPerHour = volBpsPerHour,
            volSextile = sextile,
            recommendedLeverage = scaledLeverage,
            ofi = ofi,
            moveFromVwapPct = moveFromVwapPct,
            regime = regime
        )
        latestSnapshots[symbol] = snap

        if (killSwitchEngaged) return reject("KILL SWITCH АКТИВЕН", scaledLeverage, strategyMode)
        if (window.size < 20) return reject("SCOUT: Прогрев окна (${window.size}/20)", scaledLeverage, strategyMode)
        if (hasPositionOnSymbol) return reject("RISK: Позиция по $symbol уже в работе", scaledLeverage, strategyMode)
        if (currentInventoryCount >= 3) return reject("RISK: Лимит портфеля (3/3)", scaledLeverage, strategyMode)

        if (strategyMode == 1 && recentTradesLastHour >= 4) {
            return reject("SKEPTIC KILL: Блок ячейки ликвидаций (Momentum × High-Freq)", scaledLeverage, strategyMode)
        }

        val stratName = getStrategyShortTitle(strategyMode)
        val recentTrades = window.takeLast(5)
        val recentBuy = recentTrades.filter { it.isBuy }.sumOf { it.volUsdt }
        val recentSell = recentTrades.filter { !it.isBuy }.sumOf { it.volUsdt }
        val averageTickVolume = window.map { it.volUsdt }.average().coerceAtLeast(1.0)
        val signal = StrategyCore.evaluate(
            StrategyCore.Inputs(
                mode = strategyMode,
                zScore = zScore,
                ofi = ofi,
                moveFromVwapPct = moveFromVwapPct,
                thresholdPct = thresholdPct,
                price = price,
                windowHigh = windowHigh,
                windowLow = windowLow,
                recentBuyVolume = recentBuy,
                recentSellVolume = recentSell,
                volatilitySextile = sextile,
                recentTradesLastHour = recentTradesLastHour,
                volumeRatio = volUsdt / averageTickVolume,
                whaleFlow = volUsdt >= whaleThresholdUsdt * 0.85
            )
        )
        val candidateAction = signal.action
        val customBracketMultiplier = signal.stopMultiplier

        if (candidateAction == 0) {
            return reject("SCOUT: Ожидание сетапа «$stratName»", scaledLeverage, strategyMode)
        }

        val slBps = StrategyCore.stopLossBps(thresholdPct, customBracketMultiplier)
        val tpBps = StrategyCore.takeProfitBps(slBps)

        val dynamicMargin = when (sextile) {
            1, 2 -> 120.0
            3, 4 -> 100.0
            else -> 70.0
        }

        val sideAlignChase = if (candidateAction > 0) moveFromVwapPct else -moveFromVwapPct
        val h2RiskScore = StrategyCore.h2RiskScore(sextile, scaledLeverage, sideAlignChase)

        if (h2RiskScore >= 0.72) {
            h2FlaggedOrdersCount++
            logShadowKill(symbol, candidateAction > 0, price, nowMs, "H2 Risk Score >= 72%")
            return reject("SKEPTIC KILL: Высокий риск H2 (${String.format("%.0f%%", h2RiskScore * 100)})", scaledLeverage, strategyMode)
        }

        val expectedEdge = (0.48 * tpBps) - (0.52 * slBps) - ROUNDTRIP_FEE_BPS
        val dec = QuantDecision(
            action = candidateAction,
            strategyName = stratName,
            leverage = scaledLeverage,
            marginUsdt = dynamicMargin,
            stopLossBps = slBps,
            takeProfitBps = tpBps,
            h2RiskScore = h2RiskScore,
            expectedEdgeBps = expectedEdge,
            reason = "PASS • S$sextile (${scaledLeverage}x) • $valuationTag"
        )
        latestDecisions[symbol] = dec
        return dec
    }

    private fun logShadowKill(symbol: String, wouldBeLong: Boolean, price: Double, nowMs: Long, reason: String) {
        if (shadowKills.size < 30) {
            shadowKills.add(KilledShadowTrade(symbol, wouldBeLong, price, nowMs, reason))
        }
    }

    private fun reject(reason: String, lev: Int, mode: Int): QuantDecision {
        return QuantDecision(0, getStrategyShortTitle(mode), lev, 100.0, 60.0, 120.0, 0.15, 0.0, reason)
    }

    fun recordCaptureOutcome(mfeBps: Double, realizedNetBps: Double) {
        closedMfeBpsList.add(mfeBps)
        closedRealizedBpsList.add(realizedNetBps)
        if (closedMfeBpsList.size > 100) {
            closedMfeBpsList.removeAt(0)
            closedRealizedBpsList.removeAt(0)
        }
    }

    fun getCaptureRatioPct(): Double {
        val positiveMfeIndices = closedMfeBpsList.indices.filter { closedMfeBpsList[it] >= 30.0 }
        if (positiveMfeIndices.isEmpty()) return 65.0
        val sumMfe = positiveMfeIndices.sumOf { closedMfeBpsList[it] }
        val sumReal = positiveMfeIndices.sumOf { max(closedRealizedBpsList[it], 0.0) }
        return if (sumMfe > 0) ((sumReal / sumMfe) * 100.0).coerceIn(0.0, 100.0) else 0.0
    }

    fun resetMetrics() {
        closedMfeBpsList.clear()
        closedRealizedBpsList.clear()
        shadowKills.clear()
        h2FlaggedOrdersCount = 0
        chaseBlockedCount = 0
        skepticSavedReadsCount = 0
        skepticSavedUsdtEstimate = 0.0
        killSwitchEngaged = false
    }
}
