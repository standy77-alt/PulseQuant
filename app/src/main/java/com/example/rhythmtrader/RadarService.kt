package com.example.rhythmtrader

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.widget.Toast
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.zip.GZIPInputStream
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

class RadarService : Service(), TextToSpeech.OnInitListener {

    data class CoinConfig(var thresholdPercent: Double, var whaleUsdt: Double)
    data class AlertItem(val symbol: String, val title: String, val subtitle: String, val time: String, val type: Int)
    data class StopPenalty(val wasLong: Boolean, val untilMs: Long)

    // JOURNAL Bot: Полная запись о завершенной сделке для отдельной вкладки "Портфель и Сделки"
    data class ClosedTradeRecord(
        val symbol: String,
        val isLong: Boolean,
        val marginUsdt: Double,
        val leverage: Int,
        val entryPrice: Double,
        val exitPrice: Double,
        val netPnlUsdt: Double,
        val netBps: Double,
        val mfeBps: Double,
        val reason: String,
        val openTime: String,
        val closeTime: String
    )

    data class PaperPosition(
        val symbol: String,
        val isLong: Boolean,
        val entryPrice: Double,
        val marginUsdt: Double,
        val leverage: Int,
        var takeProfitPrice: Double,
        var stopLossPrice: Double,
        var maxFavorableExcursionBps: Double = 0.0,
        var mfeLockActivated: Boolean = false,
        val h2RiskScore: Double,
        val openTime: String,
        val openTimeMs: Long = System.currentTimeMillis()
    ) {
        fun currentExcursionBps(currentPrice: Double): Double {
            val rawDiff = if (isLong) (currentPrice - entryPrice) / entryPrice else (entryPrice - currentPrice) / entryPrice
            return rawDiff * 10000.0
        }

        fun calcUnrealizedPnl(currentPrice: Double): Double {
            val posSize = marginUsdt * leverage
            val rawDiff = if (isLong) (currentPrice - entryPrice) / entryPrice else (entryPrice - currentPrice) / entryPrice
            val fee = posSize * (QuantEngine.ROUNDTRIP_FEE_BPS / 10000.0)
            return (posSize * rawDiff) - fee
        }
    }

    companion object {
        var isRunning = false
        var activeSymbols = mutableSetOf("BTC-USDT", "ETH-USDT", "SOL-USDT", "ZEC-USDT", "SUI-USDT")
        var selectedChartSymbol = "ZEC-USDT"
        var useVoice = true

        var botEnabled = true
        var botStrategyMode = 0
        var autoTradeAuthorized = false
        var authorizedStrategyMode = -1
        var autoAdaptEnabled = true
        var isAutoAdapting = false
        var lastAutoAdaptTradeCount = -1
        var lastAutoAdaptSummary = ""
        var demoBalance = 1000.0
        var totalRealizedPnl = 0.0
        var winsCount = 0
        var lossesCount = 0

        val openPositions = ConcurrentHashMap<String, PaperPosition>()
        val closedTradesHistory = CopyOnWriteArrayList<ClosedTradeRecord>() // Хранит ВСЕ закрытые сделки отдельно от сигналов!
        val coinConfigs = ConcurrentHashMap<String, CoinConfig>()
        val basePrices = ConcurrentHashMap<String, Double>()
        val lastPrices = ConcurrentHashMap<String, Double>()
        val stopPenalties = ConcurrentHashMap<String, StopPenalty>()

        val cvdBuy = ConcurrentHashMap<String, Double>()
        val cvdSell = ConcurrentHashMap<String, Double>()
        val alertHistory = CopyOnWriteArrayList<AlertItem>()
        private val tradeTimestamps = CopyOnWriteArrayList<Long>()

        var uiCallback: ((symbol: String, price: Double, volumeUsdt: Double, isBuyerMaker: Boolean, newAlert: AlertItem?) -> Unit)? = null

        fun loadBotStats(context: Context) {
            val prefs = context.getSharedPreferences("rhythm_prefs_v7", Context.MODE_PRIVATE)
            demoBalance = prefs.getFloat("demo_balance", 1000f).toDouble()
            totalRealizedPnl = prefs.getFloat("total_pnl", 0f).toDouble()
            winsCount = prefs.getInt("wins_count", 0)
            lossesCount = prefs.getInt("losses_count", 0)
            botEnabled = prefs.getBoolean("bot_enabled", true)
            botStrategyMode = prefs.getInt("bot_strategy", 0)
        }

        fun saveBotStats(context: Context) {
            val prefs = context.getSharedPreferences("rhythm_prefs_v7", Context.MODE_PRIVATE)
            prefs.edit()
                .putFloat("demo_balance", demoBalance.toFloat())
                .putFloat("total_pnl", totalRealizedPnl.toFloat())
                .putInt("wins_count", winsCount)
                .putInt("losses_count", lossesCount)
                .putBoolean("bot_enabled", botEnabled)
                .putInt("bot_strategy", botStrategyMode)
                .apply()
        }

        fun resetBotAccount(context: Context) {
            openPositions.clear()
            closedTradesHistory.clear()
            stopPenalties.clear()
            demoBalance = 1000.0
            totalRealizedPnl = 0.0
            winsCount = 0
            lossesCount = 0
            QuantEngine.resetMetrics()
            saveBotStats(context)
        }

        fun getConfig(context: Context, symbol: String): CoinConfig {
            return coinConfigs.getOrPut(symbol) {
                val prefs = context.getSharedPreferences("rhythm_prefs_v7", Context.MODE_PRIVATE)
                val defPct = when {
                    symbol.startsWith("BTC") -> 0.15f
                    symbol.startsWith("ETH") -> 0.20f
                    else -> 0.35f
                }
                val defWhale = when {
                    symbol.startsWith("BTC") -> 120000f
                    symbol.startsWith("ETH") -> 80000f
                    else -> 25000f
                }
                val pct = prefs.getFloat("pct_$symbol", defPct).toDouble()
                val whale = prefs.getFloat("whale_$symbol", defWhale).toDouble()
                CoinConfig(pct, whale)
            }
        }

        fun saveConfig(context: Context, symbol: String, pct: Double, whale: Double) {
            coinConfigs[symbol] = CoinConfig(pct, whale)
            val prefs = context.getSharedPreferences("rhythm_prefs_v7", Context.MODE_PRIVATE)
            prefs.edit()
                .putFloat("pct_$symbol", pct.toFloat())
                .putFloat("whale_$symbol", whale.toFloat())
                .apply()
        }

        fun resetBasePrice(symbol: String) {
            val last = lastPrices[symbol]
            if (last != null && last > 0) basePrices[symbol] = last
            cvdBuy[symbol] = 0.0
            cvdSell[symbol] = 0.0
            stopPenalties.remove(symbol)
        }
    }

    private var webSocket: WebSocket? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val lastAlertTime = ConcurrentHashMap<String, Long>()
    private val subscribedSymbols = mutableSetOf<String>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreate() {
        super.onCreate()
        autoTradeAuthorized = false
        authorizedStrategyMode = -1
        loadBotStats(this)
        stopPenalties.clear()
        tts = TextToSpeech(this, this)
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RhythmTrader::RadarWakeLock")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("ru", "RU"))
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            tts?.setPitch(0.95f)
            tts?.setSpeechRate(0.9f)
            tts?.setSpeechRate(1.12f)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }
        if (action == "RESUB") {
            resubscribeAll()
            return START_STICKY
        }
        if (action == "KILL_SWITCH") {
            if (QuantEngine.killSwitchEngaged) {
                for (sym in openPositions.keys.toList()) {
                    val p = lastPrices[sym] ?: openPositions[sym]?.entryPrice ?: 0.0
                    if (p > 0) closePaperTrade(sym, p, "EMERGENCY KILL SWITCH 🚨")
                }
                speakEvent("Аварийный стоп активирован")
            }
            return START_STICKY
        }
        if (action == "MANUAL_LONG" || action == "MANUAL_SHORT") {
            val sym = selectedChartSymbol
            val p = lastPrices[sym] ?: 0.0
            val snap = QuantEngine.latestSnapshots[sym]
            val lev = snap?.recommendedLeverage ?: 5
            val isLong = action == "MANUAL_LONG"

            stopPenalties.remove(sym)
            if (p > 0 && !openPositions.containsKey(sym) && !QuantEngine.killSwitchEngaged) {
                openPaperTrade(sym, isLong, p, 100.0, lev, 60.0, 120.0, 0.20, "PM Manual Ticket (Ручной вход)")
            }
            return START_STICKY
        }
        if (action == "CLOSE_POS") {
            val targetSym = intent.getStringExtra("target_symbol") ?: selectedChartSymbol
            val p = lastPrices[targetSym] ?: openPositions[targetSym]?.entryPrice ?: 0.0
            if (p > 0 && openPositions.containsKey(targetSym)) {
                val alert = closePaperTrade(targetSym, p, "Закрыто PM вручную")
                mainHandler.post {
                    uiCallback?.invoke(targetSym, p, 0.0, false, alert)
                }
            }
            return START_STICKY
        }

        for (sym in activeSymbols) getConfig(this, sym)

        startForeground(101, buildNotification("Jane Street Desk v8.0 | Капитал: $${String.format("%.2f", demoBalance)}"))
        if (wakeLock?.isHeld == false) wakeLock?.acquire(10 * 60 * 60 * 1000L)
        isRunning = true
        connectWebSocket()
        return START_STICKY
    }

    private fun connectWebSocket() {
        webSocket?.cancel()
        subscribedSymbols.clear()
        val client = OkHttpClient()
        val request = Request.Builder().url("wss://open-api-swap.bingx.com/swap-market").build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                resubscribeAll()
            }

            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                handleMessage(ungzip(bytes.toByteArray()))
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                if (isRunning) {
                    mainHandler.postDelayed({ if (isRunning) connectWebSocket() }, 3000)
                }
            }
        })
    }

    private fun resubscribeAll() {
        val ws = webSocket ?: return
        val toUnsub = subscribedSymbols.filter { !activeSymbols.contains(it) }
        for (sym in toUnsub) {
            ws.send("""{"id":"unsub_$sym","reqType":"unsub","dataType":"$sym@trade"}""")
            subscribedSymbols.remove(sym)
        }
        for (sym in activeSymbols) {
            getConfig(this, sym)
            if (!subscribedSymbols.contains(sym)) {
                ws.send("""{"id":"sub_$sym","reqType":"sub","dataType":"$sym@trade"}""")
                subscribedSymbols.add(sym)
            }
        }
    }

    private fun handleMessage(msg: String) {
        if (msg == "Ping") {
            webSocket?.send("Pong")
            return
        }
        try {
            val json = JSONObject(msg)
            val dataType = json.optString("dataType", "")
            if (!dataType.endsWith("@trade")) return
            val symbol = dataType.substringBefore("@trade")
            if (!activeSymbols.contains(symbol)) return

            val dataArr = json.optJSONArray("data") ?: return
            if (dataArr.length() > 0) {
                val trade = dataArr.getJSONObject(0)
                val price = trade.optString("p", "0").toDouble()
                val qty = trade.optString("q", "0").toDouble()
                val isBuyerMaker = trade.optBoolean("m", false)
                if (price > 0) processTrade(symbol, price, price * qty, isBuyerMaker)
            }
        } catch (_: Exception) {}
    }

    private fun processTrade(symbol: String, price: Double, volumeUsdt: Double, isBuyerMaker: Boolean) {
        lastPrices[symbol] = price
        val base = basePrices.getOrPut(symbol) { price }
        val now = System.currentTimeMillis()

        if (!isBuyerMaker) {
            cvdBuy[symbol] = (cvdBuy[symbol] ?: 0.0) + volumeUsdt
        } else {
            cvdSell[symbol] = (cvdSell[symbol] ?: 0.0) + volumeUsdt
        }

        val existingPos = openPositions[symbol]
        var generatedAlert: AlertItem? = null

        if (existingPos != null) {
            val curExcBps = existingPos.currentExcursionBps(price)
            if (curExcBps > existingPos.maxFavorableExcursionBps) {
                existingPos.maxFavorableExcursionBps = curExcBps
            }

            if (!existingPos.mfeLockActivated && existingPos.maxFavorableExcursionBps >= 45.0) {
                existingPos.mfeLockActivated = true
                existingPos.stopLossPrice = if (existingPos.isLong) {
                    existingPos.entryPrice * (1.0 + 0.0015)
                } else {
                    existingPos.entryPrice * (1.0 - 0.0015)
                }
            }

            val hitTp = if (existingPos.isLong) price >= existingPos.takeProfitPrice else price <= existingPos.takeProfitPrice
            val hitSl = if (existingPos.isLong) price <= existingPos.stopLossPrice else price >= existingPos.stopLossPrice
            // RISK Bot exit_due.py аналог: если позиция висит дольше 60 минут без движения — закрываем по времени
            val timeExpired = (now - existingPos.openTimeMs) >= 60 * 60_000L

            if (hitTp) {
                stopPenalties.remove(symbol)
                generatedAlert = closePaperTrade(symbol, price, "BRACKET TARGET 🎯")
            } else if (hitSl) {
                if (!existingPos.mfeLockActivated) {
                    stopPenalties[symbol] = StopPenalty(wasLong = existingPos.isLong, untilMs = now + 90_000L)
                }
                val tag = if (existingPos.mfeLockActivated) "MFE PROFIT LOCK 🔒" else "BRACKET STOP "
                generatedAlert = closePaperTrade(symbol, price, tag)
            } else if (timeExpired) {
                generatedAlert = closePaperTrade(symbol, price, "TIME-DECAY EXIT ⏱️ (60m Limit)")
            }
        }

        val cfg = getConfig(this, symbol)
        val cooldownOk = (now - (lastAlertTime[symbol] ?: 0L)) > 3000L

        tradeTimestamps.removeAll { now - it > 3600_000L }

        val decision = QuantEngine.recordTickAndEvaluate(
            symbol = symbol,
            price = price,
            volUsdt = volumeUsdt,
            isBuy = !isBuyerMaker,
            nowMs = now,
            strategyMode = botStrategyMode,
            thresholdPct = cfg.thresholdPercent,
            whaleThresholdUsdt = cfg.whaleUsdt,
            currentInventoryCount = openPositions.size,
            hasPositionOnSymbol = openPositions.containsKey(symbol),
            recentTradesLastHour = tradeTimestamps.size
        )

        val diffPercent = ((price - base) / base) * 100.0
        val snap = QuantEngine.latestSnapshots[symbol]
        val ofi = snap?.ofi ?: 0.0

        var finalAction = decision.action
        var finalReason = decision.reason
        if (!QuantEngine.killSwitchEngaged && finalAction == 0 && abs(diffPercent) >= cfg.thresholdPercent && !openPositions.containsKey(symbol) && openPositions.size < 3) {
            if (diffPercent > 0 && ofi >= 0.15 && (snap?.moveFromVwapPct ?: 0.0) <= 0.85) {
                finalAction = 1
                finalReason = "Breakout Impulse ▲ (${String.format("+%.2f%%", diffPercent)} + OFI ${String.format("+%.2f", ofi)})"
            } else if (diffPercent < 0 && ofi <= -0.15 && (snap?.moveFromVwapPct ?: 0.0) >= -0.85) {
                finalAction = -1
                finalReason = "Breakout Impulse ▼ (${String.format("%.2f%%", diffPercent)} + OFI ${String.format("%.2f", ofi)})"
            }
        }

        val isLongSignal = finalAction > 0

        val penalty = stopPenalties[symbol]
        val inSameSideCooldown = if (penalty != null && now < penalty.untilMs) {
            if (penalty.wasLong == isLongSignal && abs(ofi) < 0.35) true else false
        } else {
            if (penalty != null) stopPenalties.remove(symbol)
            false
        }

        val btcSnap = QuantEngine.latestSnapshots["BTC-USDT"]
        val btcCrashingHard = symbol != "BTC-USDT" && btcSnap != null && btcSnap.zScore < -1.8 && btcSnap.ofi < -0.35
        val blockedByBtc = isLongSignal && btcCrashingHard

        if (botEnabled && autoTradeAuthorized && authorizedStrategyMode == botStrategyMode && !QuantEngine.killSwitchEngaged && finalAction != 0 && !openPositions.containsKey(symbol) && openPositions.size < 3 && cooldownOk && demoBalance >= decision.marginUsdt && !inSameSideCooldown && !blockedByBtc) {
            lastAlertTime[symbol] = now
            basePrices[symbol] = price
            tradeTimestamps.add(now)
            generatedAlert = openPaperTrade(
                symbol = symbol,
                isLong = isLongSignal,
                price = price,
                marginUsdt = decision.marginUsdt,
                leverage = decision.leverage,
                slBps = decision.stopLossBps,
                tpBps = decision.takeProfitBps,
                h2Score = decision.h2RiskScore,
                reason = finalReason
            )
        } else if (abs(diffPercent) >= cfg.thresholdPercent && cooldownOk) {
            val isPump = diffPercent > 0
            basePrices[symbol] = price
            lastAlertTime[symbol] = now
            val shortName = symbol.replace("-USDT", "")
            if (generatedAlert == null) {
                val remainSec = if (penalty != null && now < penalty.untilMs) ((penalty.untilMs - now) / 1000).toInt() else 0
                val vetoNote = when {
                    blockedByBtc -> " [BTC Crash Filter]"
                    inSameSideCooldown -> " [Пауза ${remainSec}с]"
                    openPositions.containsKey(symbol) -> " [Уже в сделке]"
                    openPositions.size >= 3 -> " [Лимит 3/3 поз.]"
                    else -> " • ${decision.reason}"
                }
                generatedAlert = AlertItem(
                    symbol = symbol,
                    title = "$shortName  Движение ${if (isPump) "▲" else "▼"} ${String.format("%+.2f%%", diffPercent)}",
                    subtitle = "Цена: $price$vetoNote",
                    time = timeFormat.format(Date(now)),
                    type = if (isPump) 1 else 2
                )
                addAlert(generatedAlert)
                playSynth(if (isPump) 1 else 2)
            }
        }

        mainHandler.post {
            uiCallback?.invoke(symbol, price, volumeUsdt, isBuyerMaker, generatedAlert)
        }
    }

    private fun openPaperTrade(
        symbol: String,
        isLong: Boolean,
        price: Double,
        marginUsdt: Double,
        leverage: Int,
        slBps: Double,
        tpBps: Double,
        h2Score: Double,
        reason: String
    ): AlertItem {
        val tp = if (isLong) price * (1.0 + tpBps / 10000.0) else price * (1.0 - tpBps / 10000.0)
        val sl = if (isLong) price * (1.0 - slBps / 10000.0) else price * (1.0 + slBps / 10000.0)
        val nowStr = timeFormat.format(Date())

        val pos = PaperPosition(
            symbol = symbol,
            isLong = isLong,
            entryPrice = price,
            marginUsdt = marginUsdt,
            leverage = leverage,
            takeProfitPrice = tp,
            stopLossPrice = sl,
            maxFavorableExcursionBps = 0.0,
            mfeLockActivated = false,
            h2RiskScore = h2Score,
            openTime = nowStr,
            openTimeMs = System.currentTimeMillis()
        )
        openPositions[symbol] = pos

        val shortName = symbol.replace("-USDT", "")
        val sideStr = if (isLong) "LONG 🟢" else "SHORT 🔴"
        val alert = AlertItem(
            symbol = symbol,
            title = "$sideStr [$shortName] $${marginUsdt.toInt()} × ${leverage}x",
            subtitle = "$reason",
            time = nowStr,
            type = if (isLong) 1 else 2
        )
        addAlert(alert)
        playSynth(if (isLong) 1 else 2)
        speakEvent("Вход ${if (isLong) "лонг" else "шорт"} по $shortName")
        updateNotification("В работе: ${openPositions.size} | Капитал: $${String.format("%.2f", demoBalance)}")
        return alert
    }

    private fun closePaperTrade(symbol: String, closePrice: Double, reason: String): AlertItem? {
        val pos = openPositions.remove(symbol) ?: return null
        val pnl = pos.calcUnrealizedPnl(closePrice)
        val netRealizedBps = pos.currentExcursionBps(closePrice) - QuantEngine.ROUNDTRIP_FEE_BPS
        val mfeBps = max(pos.maxFavorableExcursionBps, max(netRealizedBps, 0.0))

        demoBalance += pnl
        totalRealizedPnl += pnl
        val won = pnl >= 0
        if (won) winsCount++ else lossesCount++

        val closeTimeStr = timeFormat.format(Date())

        // Сохраняем в отдельный журнал завершенных сделок (до 100 последних сделок)
        closedTradesHistory.add(
            0,
            ClosedTradeRecord(
                symbol = symbol,
                isLong = pos.isLong,
                marginUsdt = pos.marginUsdt,
                leverage = pos.leverage,
                entryPrice = pos.entryPrice,
                exitPrice = closePrice,
                netPnlUsdt = pnl,
                netBps = netRealizedBps,
                mfeBps = mfeBps,
                reason = reason,
                openTime = pos.openTime,
                closeTime = closeTimeStr
            )
        )
        while (closedTradesHistory.size > 100) {
            closedTradesHistory.removeAt(closedTradesHistory.size - 1)
        }

        QuantEngine.recordCaptureOutcome(mfeBps, netRealizedBps)
        saveBotStats(this)

        val shortName = symbol.replace("-USDT", "")
        val pnlStr = String.format("%+.2f$", pnl)
        val alert = AlertItem(
            symbol = symbol,
            title = "🏁 ЗАКРЫТ ${if (pos.isLong) "LONG" else "SHORT"} [$shortName]  $pnlStr",
            subtitle = "$reason • MFE: +${mfeBps.toInt()} bps",
            time = closeTimeStr,
            type = if (won) 1 else 2
        )
        addAlert(alert)
        playSynth(if (won) 1 else 2)
        speakEvent("Сделка по $shortName закрыта")
        updateNotification("Капитал: $${String.format("%.2f", demoBalance)} (Сделок: ${winsCount + lossesCount})")
        return alert
    }

    private fun addAlert(item: AlertItem) {
        alertHistory.add(0, item)
        while (alertHistory.size > 25) alertHistory.removeAt(alertHistory.size - 1)
    }

    private fun speakEvent(text: String) {
        if (!useVoice || !ttsReady) return
        val clean = text.replace("BTC", "Биткоин").replace("ETH", "Эфир").replace("SOL", "Солана").replace("ZEC", "Зикэш").replace("SUI", "Суи")
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "alert_id")
    }

    fun playSynth(type: Int) {
        thread {
            try {
                val sampleRate = 44100
                val durationMs = if (type == 3) 240 else 340
                val numSamples = (sampleRate * durationMs) / 1000
                val samples = ShortArray(numSamples)
                val freqs = when (type) {
                    1 -> doubleArrayOf(523.25, 659.25, 783.99)
                    2 -> doubleArrayOf(196.00, 146.83, 110.00)
                    else -> doubleArrayOf(880.0, 1174.66, 880.0)
                }
                val step = numSamples / 3
                for (i in 0 until numSamples) {
                    val f = freqs[(i / step).coerceAtMost(2)]
                    val angle = 2.0 * Math.PI * i * f / sampleRate
                    val localPos = (i % step).toDouble() / step
                    val env = (1.0 - localPos * 0.8)
                    samples[i] = (sin(angle) * Short.MAX_VALUE * 0.55 * env).toInt().toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(samples, 0, samples.size)
                track.play()
                Thread.sleep(durationMs.toLong() + 80)
                track.release()
            } catch (_: Exception) {}
        }
    }

    private fun buildNotification(text: String): Notification {
        val channelId = "rhythm_radar_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "RhythmQuant Jane Street Desk", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val openIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, channelId) else Notification.Builder(this)
        return builder.setContentTitle("PulseQuant // Paper Trading").setContentText(text).setSmallIcon(android.R.drawable.ic_lock_silent_mode_off).setContentIntent(openIntent).setOngoing(true).build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(101, buildNotification(text))
    }

    private fun ungzip(content: ByteArray): String {
        return try {
            GZIPInputStream(ByteArrayInputStream(content)).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            String(content)
        }
    }

    override fun onDestroy() {
        isRunning = false
        webSocket?.close(1000, "Service destroyed")
        if (wakeLock?.isHeld == true) wakeLock?.release()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
