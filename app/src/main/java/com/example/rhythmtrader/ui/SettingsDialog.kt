package com.example.rhythmtrader.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
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

        // === Оформление ===
        root.addView(sectionTitle(context, "Оформление"))
        root.addView(switchRow(context, "Тёмная тема", AppTheme.isLight.not()) { checked ->
            AppTheme.isLight = !checked
            onThemeChanged()
        })
        addSpace(root, 24)

        // === Голос ===
        root.addView(sectionTitle(context, "Голосовые уведомления"))
        root.addView(switchRow(context, "Озвучивать сигналы", RadarService.useVoice) { checked ->
            RadarService.useVoice = checked
        })
        addSpace(root, 24)

        // === Авто-адаптация ===
        root.addView(sectionTitle(context, "Авто-адаптация стратегий"))
        root.addView(switchRow(context, "Авто-адапт включён", RadarService.autoAdaptEnabled) { checked ->
            RadarService.autoAdaptEnabled = checked
        })
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
                val symbols = RadarService.openPositions.keys.toList()
                for (sym in symbols) {
                    val price = RadarService.lastPrices[sym]
                        ?: RadarService.openPositions[sym]?.entryPrice
                        ?: 0.0
                    if (price > 0) {
                        val intent = Intent(context, RadarService::class.java).apply {
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
        root.addView(UiHelpers.createSubtitle(context,
            "PulseQuant — paper trading lab\nСтратегии используют shared StrategyCore + QuantEngine"))

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
        dialog.window?.setBackgroundDrawable(UiHelpers.rounded(AppTheme.bgCard(), 28f))
    }

    /** Строка: текст слева, Switch справа */
    private fun switchRow(
        context: Context,
        label: String,
        initialChecked: Boolean,
        onChecked: (Boolean) -> Unit
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 12, 0, 12)

            addView(TextView(context).apply {
                text = label
                textSize = 15f
                setTextColor(AppTheme.textPrimary())
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })

            addView(Switch(context).apply {
                isChecked = initialChecked
                setOnCheckedChangeListener { _, checked -> onChecked(checked) }
            })
        }
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
