package com.example.rhythmtrader.ui

import android.graphics.Color

object AppTheme {
    var isLight = false

    // === Фоны и текст ===
    fun bgMain(): Int = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#0A0D12")
    fun bgCard(): Int = if (isLight) Color.parseColor("#FFFFFF") else Color.parseColor("#151A24")
    fun bgSubCard(): Int = if (isLight) Color.parseColor("#F2F2F7") else Color.parseColor("#1E2530")
    fun stroke(): Int = if (isLight) Color.parseColor("#E5E5EA") else Color.parseColor("#252B38")

    fun textPrimary(): Int = if (isLight) Color.parseColor("#1C1C1E") else Color.WHITE
    fun textSecondary(): Int = if (isLight) Color.parseColor("#6C6C70") else Color.parseColor("#8892A6")
    fun textTertiary(): Int = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#5A6478")

    // === Акценты ===
    fun accent(): Int = if (isLight) Color.parseColor("#5A7B9A") else Color.parseColor("#7A9BC0")
    fun green(): Int = Color.parseColor("#34C759")
    fun red(): Int = Color.parseColor("#FF453A")
    fun orange(): Int = Color.parseColor("#FF9F0A")
    fun purple(): Int = Color.parseColor("#BF5AF2")

    // === Кнопки ===
    fun btnPrimaryBg(): Int = accent()
    fun btnSecondaryBg(): Int = if (isLight) Color.parseColor("#E5E5EA") else Color.parseColor("#1E2530")
    fun btnDangerBg(): Int = red()

    // === Совместимость со старым кодом ===
    // Зелёные — сводим к 2 оттенкам
    fun greenDeep(): Int = Color.parseColor("#2E7D32")     // PnL фон, победа
    fun greenLight(): Int = Color.parseColor("#34C759")    // акцентный
    fun greenDark(): Int = Color.parseColor("#2E7D32")     // было 1B5E20 → стало как greenDeep

    // Красные — сводим к 2 оттенкам
    fun redDeep(): Int = Color.parseColor("#C62828")       // PnL фон, убыток
    fun redDark(): Int = Color.parseColor("#C62828")       // было B71C1C → redDeep
    fun redLight(): Int = Color.parseColor("#FF453A")      // было EF5350 → red
    fun redVeryDark(): Int = Color.parseColor("#C62828")   // было 8E0000 → redDeep

    // Синие — сводим к акценту
    fun blueLight(): Int = accent()                         // было 42A5F5
    fun blueDeep(): Int = accent()                          // было 0D47A1

    // Фиолетовые — убираем, заменяем на акцент
    fun purpleDeep(): Int = accent()                        // было 5E35B1
    fun purpleLight(): Int = accent()                       // было 9575CD

    // Оранжевые — убираем, заменяем на красный
    fun orangeDeep(): Int = redDeep()                       // было EF6C00
    fun orangeVeryDark(): Int = redDark()                   // было 8A5A00

    // Серые (кнопка "Закрыть позицию")
    fun grayButton(): Int = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#3A4256")
    fun grayButtonStroke(): Int = if (isLight) Color.parseColor("#AEAEB2") else Color.parseColor("#4A5468")
}
