package com.example.rhythmtrader

import kotlin.math.abs
import kotlin.math.max

/** Pure, deterministic signal rules shared by live paper trading and historical simulation. */
object StrategyCore {
    data class Inputs(
        val mode: Int,
        val zScore: Double,
        val ofi: Double,
        val moveFromVwapPct: Double,
        val thresholdPct: Double,
        val price: Double,
        val windowHigh: Double,
        val windowLow: Double,
        val recentBuyVolume: Double,
        val recentSellVolume: Double,
        val volatilitySextile: Int,
        val recentTradesLastHour: Int,
        val volumeRatio: Double = 0.0,
        val whaleFlow: Boolean = false
    )

    data class Signal(val action: Int, val stopMultiplier: Double = 1.0, val reason: String = "No setup")

    fun evaluate(input: Inputs): Signal {
        val triggerZ = max(input.thresholdPct * 4.5, 1.30)
        val z = input.zScore
        val ofi = input.ofi
        val move = input.moveFromVwapPct
        return when (input.mode) {
            0 -> when {
                z <= -triggerZ && ofi > -0.60 -> Signal(1, reason = "Mean reversion: below VWAP")
                z >= triggerZ && ofi < 0.60 -> Signal(-1, reason = "Mean reversion: above VWAP")
                else -> Signal(0)
            }
            1 -> when {
                input.recentTradesLastHour >= 4 -> Signal(0, reason = "Momentum trade-frequency limit")
                z >= triggerZ * 0.85 && ofi >= 0.30 && move <= 0.75 -> Signal(1, reason = "Momentum long")
                z <= -triggerZ * 0.85 && ofi <= -0.30 && move >= -0.75 -> Signal(-1, reason = "Momentum short")
                else -> Signal(0)
            }
            2 -> when {
                input.price <= input.windowLow && z <= -1.2 && input.recentBuyVolume > input.recentSellVolume * 1.5 -> Signal(1, reason = "Squeeze reversal long")
                input.price >= input.windowHigh && z >= 1.2 && input.recentSellVolume > input.recentBuyVolume * 1.5 -> Signal(-1, reason = "Squeeze reversal short")
                else -> Signal(0)
            }
            3 -> when {
                input.volatilitySextile <= 4 && input.recentTradesLastHour < 2 && abs(z) >= triggerZ * 1.1 && ofi >= 0.45 && z > 0 -> Signal(1, 1.5, "Patient sniper long")
                input.volatilitySextile <= 4 && input.recentTradesLastHour < 2 && abs(z) >= triggerZ * 1.1 && ofi <= -0.45 && z < 0 -> Signal(-1, 1.5, "Patient sniper short")
                else -> Signal(0)
            }
            else -> when {
                input.whaleFlow && ofi >= 0.25 && move < 0.70 -> Signal(1, reason = "Whale flow long")
                input.whaleFlow && ofi <= -0.25 && move > -0.70 -> Signal(-1, reason = "Whale flow short")
                input.volumeRatio >= 1.8 -> Signal(if (input.ofi >= 0) 1 else -1, reason = "High-volume flow")
                else -> Signal(0)
            }
        }
    }

    fun h2RiskScore(volatilitySextile: Int, leverage: Int, sideAlignedMovePct: Double): Double = (
        (volatilitySextile / 6.0) * 0.45 +
            (leverage / 10.0) * 0.25 +
            max(sideAlignedMovePct, 0.0) * 0.30
    ).coerceIn(0.05, 0.99)

    fun stopLossBps(thresholdPct: Double, stopMultiplier: Double = 1.0): Double =
        (thresholdPct * 100.0 * 1.2 * stopMultiplier).coerceIn(45.0, 220.0)

    fun takeProfitBps(stopLossBps: Double): Double = stopLossBps * 2.0
}
