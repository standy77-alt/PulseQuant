package com.example.rhythmtrader

import org.junit.Assert.assertEquals
import org.junit.Test

class StrategyCoreTest {
    private fun inputs(mode: Int, z: Double, ofi: Double, move: Double = 0.0) = StrategyCore.Inputs(
        mode = mode, zScore = z, ofi = ofi, moveFromVwapPct = move, thresholdPct = 0.35,
        price = 100.0, windowHigh = 101.0, windowLow = 99.0,
        recentBuyVolume = 100.0, recentSellVolume = 10.0,
        volatilitySextile = 2, recentTradesLastHour = 0, volumeRatio = 2.0, whaleFlow = true
    )

    @Test fun meanReversionUsesCanonicalThreshold() {
        assertEquals(1, StrategyCore.evaluate(inputs(0, -1.6, 0.0)).action)
        assertEquals(0, StrategyCore.evaluate(inputs(0, -1.0, 0.0)).action)
    }

    @Test fun momentumBlocksChasing() {
        assertEquals(1, StrategyCore.evaluate(inputs(1, 1.4, 0.4, 0.4)).action)
        assertEquals(0, StrategyCore.evaluate(inputs(1, 1.4, 0.4, 0.9)).action)
    }

    @Test fun sniperUsesWiderStopMultiplier() {
        val signal = StrategyCore.evaluate(inputs(3, 2.0, 0.5))
        assertEquals(1, signal.action)
        assertEquals(1.5, signal.stopMultiplier, 0.0)
    }

    @Test fun bracketsAreSharedAndBounded() {
        assertEquals(45.0, StrategyCore.stopLossBps(0.15), 0.0)
        assertEquals(84.0, StrategyCore.stopLossBps(0.70), 0.0)
        assertEquals(168.0, StrategyCore.takeProfitBps(84.0), 0.0)
        assertEquals(220.0, StrategyCore.stopLossBps(5.0), 0.0)
    }
}
