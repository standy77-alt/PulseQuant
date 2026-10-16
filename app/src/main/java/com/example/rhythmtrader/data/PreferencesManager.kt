package com.example.rhythmtrader.data

import android.content.Context
import com.example.rhythmtrader.RadarService

object PreferencesManager {

    private const val PREFS_NAME = "rhythm_prefs_v7"
    private const val KEY_SLOT_COINS = "slot_coins"
    private const val KEY_LIGHT_THEME = "light_theme"
    const val SLOT_COUNT = 5

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveCoins(context: Context, slotCoins: List<String>) {
        prefs(context).edit()
            .putString(KEY_SLOT_COINS, slotCoins.joinToString(","))
            .apply()
    }

    fun saveTheme(context: Context, isLightTheme: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_LIGHT_THEME, isLightTheme)
            .apply()
    }

    /**
     * Загружает тему и список слотов. Возвращает Pair(isLightTheme, slotCoins).
     * Также синхронизирует RadarService.activeSymbols со слотами.
     */
    fun load(context: Context, defaultCoins: List<String>): Pair<Boolean, List<String>> {
        val p = prefs(context)
        val isLight = p.getBoolean(KEY_LIGHT_THEME, false)
        val saved = p.getString(KEY_SLOT_COINS, null)

        val coins = if (!saved.isNullOrEmpty()) {
            val parts = saved.split(",")
            if (parts.size == SLOT_COUNT) parts.toList() else defaultCoins
        } else {
            defaultCoins
        }

        RadarService.activeSymbols.clear()
        RadarService.activeSymbols.addAll(coins)

        return Pair(isLight, coins)
    }
}
