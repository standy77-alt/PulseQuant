package com.example.rhythmtrader.ui

import android.graphics.Color

object AppTheme {
    var isLight = false

    // === Фоны и текст ===
    fun bgMain(): Int = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#000000")
    fun bgCard(): Int = if (isLight) Color.parseColor("#FFFFFF") else Color.parseColor("#1C1C1E")
    fun bgSubCard(): Int = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#2C2C2E")
    fun stroke(): Int = if (isLight) Color.parseColor("#D1D1D6") else Color.parseColor("#3A3A3C")

    fun textPrimary(): Int = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    fun textSecondary(): Int = if (isLight) Color.parseColor("#6C6C70") else Color.parseColor("#AEAEB2")
    fun textTertiary(): Int = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#636366")

    // === Акценты ===
    fun accent(): Int = Color.parseColor("#0A84FF")
    fun green(): Int = Color.parseColor("#30D158")
    fun red(): Int = Color.parseColor("#FF453A")
    fun orange(): Int = Color.parseColor("#FF9F0A")
    fun purple(): Int = Color.parseColor("#BF5AF2")

    // === Кнопки ===
    fun btnPrimaryBg(): Int = accent()
    fun btnSecondaryBg(): Int = if (isLight) Color.parseColor("#E5E5EA") else Color.parseColor("#2C2C2E")
    fun btnDangerBg(): Int = red()

    // === Совместимость со старым кодом ===
    // Зелёные
    fun greenDeep(): Int = Color.parseColor("#2E7D32")     // PnL фон, победа
    fun greenLight(): Int = Color.parseColor("#4CAF50")
    fun greenDark(): Int = Color.parseColor("#1B5E20")

    // Красные
    fun redDeep(): Int = Color.parseColor("#C62828")       // PnL фон, убыток
    fun redDark(): Int = Color.parseColor("#B71C1C")       // LONG/SHORT и Stop
    fun redLight(): Int = Color.parseColor("#EF5350")
    fun redVeryDark(): Int = Color.parseColor("#8E0000")   // Kill Switch фон

    // Синие
    fun blueLight(): Int = Color.parseColor("#42A5F5")
    fun blueDeep(): Int = Color.parseColor("#0D47A1")

    // Фиолетовые
    fun purpleDeep(): Int = Color.parseColor("#5E35B1")
    fun purpleLight(): Int = Color.parseColor("#9575CD")

    // Оранжевые
    fun orangeDeep(): Int = Color.parseColor("#EF6C00")
    fun orangeVeryDark(): Int = Color.parseColor("#8A5A00")

    // Серые (кнопка "Закрыть позицию")
    fun grayButton(): Int = Color.parseColor("#48484A")
    fun grayButtonStroke(): Int = Color.parseColor("#636366")
}
