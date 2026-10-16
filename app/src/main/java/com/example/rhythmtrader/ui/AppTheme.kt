package com.example.rhythmtrader.ui

import android.graphics.Color

object AppTheme {

    var isLight = false

    // Основные цвета
    fun bgMain(): Int = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#000000")
    fun bgCard(): Int = if (isLight) Color.parseColor("#FFFFFF") else Color.parseColor("#1C1C1E")
    fun bgSubCard(): Int = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#2C2C2E")
    fun stroke(): Int = if (isLight) Color.parseColor("#D1D1D6") else Color.parseColor("#3A3A3C")

    fun textPrimary(): Int = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    fun textSecondary(): Int = if (isLight) Color.parseColor("#6C6C70") else Color.parseColor("#AEAEB2")
    fun textTertiary(): Int = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#636366")

    // Акценты
    fun accent(): Int = Color.parseColor("#0A84FF")
    fun green(): Int = Color.parseColor("#30D158")
    fun red(): Int = Color.parseColor("#FF453A")
    fun orange(): Int = Color.parseColor("#FF9F0A")
    fun purple(): Int = Color.parseColor("#BF5AF2")

    // Кнопки
    fun btnPrimaryBg(): Int = accent()
    fun btnSecondaryBg(): Int = if (isLight) Color.parseColor("#E5E5EA") else Color.parseColor("#2C2C2E")
    fun btnDangerBg(): Int = Color.parseColor("#FF453A")
}

