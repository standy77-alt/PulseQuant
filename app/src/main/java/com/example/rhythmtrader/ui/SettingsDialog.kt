package com.example.rhythmtrader.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import com.example.rhythmtrader.QuantEngine
import com.example.rhythmtrader.RadarService

object SettingsDialog {

    fun show(context: Context, onThemeChanged: () -> Unit, onResetAccount: () -> Unit) {
        val scroll = ScrollView(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 32, 36, 40)
        }
        scroll.addView(root)

        // Заголовок
        root.addView(TextView(context).apply {
            text = "Настройки"
            textSize = 22f
            setTextColor(AppTheme.textPrimary())
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, 0, 0, 28)
        })

        // === Тема ===
        root.addView(sectionTitle(context, "Оформление"))
        val themeBtn = UiHelpers.createSecondaryButton(context,
            if (AppTheme.isLight) "Переключить на тёмную тему" else "Переключить на светлую тему"
        )
        themeBtn.setOnClickListener {
            AppTheme.isLight = !AppTheme.isLight
            onThemeChanged()
            Toast.makeText(context, if (AppTheme.isLight) "Светлая тема" else "Тёмная тема", Toast.LENGTH_SHORT).show()
        }
        root.addView(themeBtn)
        addSpace(root, 24)

        // === Голос ===
        root.addView(sectionTitle(context, "Голосовые уведомления"))
        val voiceBtn = UiHelpers.createSecondaryButton(context,
            if (RadarService.useVoice) "Голос: Включён" else "Голос: Выключен"
        )
        voiceBtn.setOnClickListener {
            RadarService.useVoice = !RadarService.useVoice
            voiceBtn.text = if (RadarService.useVoice) "Голос: Включён" else "Голос: Выключен"
        }
        root.addView(voiceBtn)
        addSpace(root, 24)

        // === Авто-адаптация ===
        root.addView(sectionTitle(context, "Авто-адаптация стратегий"))
        val adaptBtn = UiHelpers.createSecondaryButton(context,
            if (RadarService.autoAdaptEnabled) "Авто-адапт: Включён" else "Авто-адапт: Выключен"
        )
        adaptBtn.setOnClickListener {
            RadarService.autoAdaptEnabled = !RadarService.autoAdaptEnabled
            adaptBtn.text = if (RadarService.autoAdaptEnabled) "Авто-адапт: Включён" else "Авто-адапт: Выключен"
        }
        root.addView(adaptBtn)
        addSpace(root, 24)

        // === Kill Switch ===
        root.addView(sectionTitle(context, "Безопасность"))
        val killBtn = UiHelpers.createDangerButton(context,
            if (QuantEngine.killSwitchEngaged) "Kill Switch: АКТИВЕН" else "Активировать Kill Switch"
        )
        killBtn.setOnClickListener {
            val turningOn = !QuantEngine.killSwitchEngaged
            QuantEngine.killSwitchEngaged = turningOn
            killBtn.text = if (turningOn) "Kill Switch: АКТИВЕН" else "Активировать Kill Switch"

            if (turningOn) {
                // Закрываем все открытые позиции
                val symbols = RadarService.openPositions.keys.toList()
                for (sym in symbols) {
                    val price = RadarService.lastPrices[sym] 
                        ?: RadarService.openPositions[sym]?.entryPrice 
                        ?: 0.0
                    if (price > 0) {
                        // Используем существующий механизм закрытия через сервис
                        val intent = android.content.Intent(context, RadarService::class.java).apply {
                            action = "CLOSE_POS"
                            putExtra("target_symbol", sym)
                        }
                        context.startService(intent)
                    }
                }
                Toast.makeText(context, "Аварийный стоп: позиции закрываются", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Kill Switch выключен", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(killBtn)
        addSpace(root, 24)

        // === Сброс счёта ===
        root.addView(sectionTitle(context, "Бумажный счёт"))
        val resetBtn = UiHelpers.createDangerButton(context, "Сбросить бумажный счёт")
        resetBtn.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Сброс счёта")
                .setMessage("Обнулить баланс, историю сделок и статистику?")
                .setPositiveButton("Сбросить") { _, _ ->
                    onResetAccount()
                    Toast.makeText(context, "Счёт сброшен", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
        root.addView(resetBtn)
        addSpace(root, 32)

        // === Информация ===
        root.addView(sectionTitle(context, "Информация"))
        root.addView(UiHelpers.createSubtitle(context, "PulseQuant — paper trading lab\nСтратегии используют shared StrategyCore + QuantEngine"))

        // === Help ===
        addSpace(root, 24)
        val helpBtn = UiHelpers.createSecondaryButton(context, "❓  Как пользоваться")
        helpBtn.setOnClickListener { HelpDialog.show(context) }
        root.addView(helpBtn)

        val dialog = AlertDialog.Builder(context)
            .setView(scroll)
            .setNegativeButton("Закрыть", null)
            .create()

        dialog.show()

        // Подгоняем цвета диалога под тему
        dialog.window?.setBackgroundDrawable(UiHelpers.rounded(AppTheme.bgCard(), 28f))
    }

    private fun sectionTitle(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 13f
            setTextColor(AppTheme.textSecondary())
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, 0, 0, 12)
        }
    }

    private fun addSpace(parent: LinearLayout, height: Int) {
        parent.addView(Space(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        })
    }
}
