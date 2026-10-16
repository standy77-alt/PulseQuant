package com.example.rhythmtrader.ui

import android.graphics.Color

object AppTheme {
    var isLight = false

    // === Фоны и текст ===
    fun bgMain(): Int = if (isLight) Color.parseColor("#F4F6F8") else Color.parseColor("#0F1620")
    fun bgCard(): Int = if (isLight) Color.parseColor("#FFFFFF") else Color.parseColor("#1A2332")
    fun bgSubCard(): Int = if (isLight) Color.parseColor("#E7ECF2") else Color.parseColor("#232E42")
    fun stroke(): Int = if (isLight) Color.parseColor("#E7ECF2") else Color.parseColor("#2C3849")

    fun textPrimary(): Int = if (isLight) Color.parseColor("#263B50") else Color.WHITE
    fun textSecondary(): Int = if (isLight) Color.parseColor("#7A8FA8") else Color.parseColor("#8892A6")
    fun textTertiary(): Int = if (isLight) Color.parseColor("#A8B8C8") else Color.parseColor("#5A6478")

    // === Акценты ===
    // accent — ОСНОВНОЙ: кнопки, заголовки, активные вкладки (графитовый)
    fun accent(): Int = if (isLight) Color.parseColor("#2E4152") else Color.parseColor("#5A7B9A")

    // secondary — ВТОРИЧНЫЙ: ползунки, ссылки, мелкие акценты (голубой)
    fun secondary(): Int = if (isLight) Color.parseColor("#6B9DE8") else Color.parseColor("#7FB0F0")

    // green — прибыль и активные состояния
    fun green(): Int = Color.parseColor("#35B88A")

    // red — убыток и опасные действия
    fun red(): Int = if (isLight) Color.parseColor("#E5484D") else Color.parseColor("#FF5B60")

    fun orange(): Int = Color.parseColor("#FF9F0A")
    fun purple(): Int = Color.parseColor("#BF5AF2")

    // === Кнопки ===
    // Главные кнопки — графитовый
    fun btnPrimaryBg(): Int = if (isLight) Color.parseColor("#2E4152") else Color.parseColor("#4A6B8E")
    fun btnSecondaryBg(): Int = if (isLight) Color.parseColor("#E7ECF2") else Color.parseColor("#232E42")
    fun btnDangerBg(): Int = red()

    // === Совместимость со старым кодом ===
    // Зелёные — всё в #35B88A (прибыль/победа)
    fun greenDeep(): Int = Color.parseColor("#35B88A")
    fun greenLight(): Int = Color.parseColor("#35B88A")
    fun greenDark(): Int = Color.parseColor("#35B88A")

    // Красные — к 2 оттенкам
    fun redDeep(): Int = red()
    fun redDark(): Int = red()
    fun redLight(): Int = if (isLight) Color.parseColor("#FF6B70") else Color.parseColor("#FF7B80")
    fun redVeryDark(): Int = if (isLight) Color.parseColor("#C33A3F") else Color.parseColor("#B8383D")

    // Синие — blueDeep для главных кнопок (графитовый), blueLight для обводок
    fun blueDeep(): Int = if (isLight) Color.parseColor("#2E4152") else Color.parseColor("#4A6B8E")
    fun blueLight(): Int = if (isLight) Color.parseColor("#3A5570") else Color.parseColor("#6A8BAE")

    // Фиолетовые — убираем, заменяем на accent (графитовый)
    fun purpleDeep(): Int = accent()
    fun purpleLight(): Int = accent()

    // Оранжевые — убираем, заменяем на красный
    fun orangeDeep(): Int = red()
    fun orangeVeryDark(): Int = redDeep()

    // Серые (кнопка "Закрыть позицию")
    fun grayButton(): Int = if (isLight) Color.parseColor("#8E8E93") else Color.parseColor("#3A4256")
    fun grayButtonStroke(): Int = if (isLight) Color.parseColor("#AEAEB2") else Color.parseColor("#4A5468")
}
