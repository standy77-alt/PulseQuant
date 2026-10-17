package com.example.rhythmtrader.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.example.rhythmtrader.QuantEngine
import com.example.rhythmtrader.RadarService

object SettingsDialog {

    private const val ROW_HEIGHT_DP = 48

    fun show(context: Context, onThemeChanged: () -> Unit, onResetAccount: () -> Unit) {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        var dialogRef: AlertDialog? = null

        val scroll = ScrollView(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(8))
            setBackgroundColor(AppTheme.bgCard())
        }
        scroll.addView(root)

        // ========== HEADER ==========
        root.addView(TextView(context).apply {
            text = "Настройки"
            textSize = 19f
            setTextColor(AppTheme.textPrimary())
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        root.addView(TextView(context).apply {
            text = "Персонализация и управление"
            textSize = 11f
            setTextColor(AppTheme.textSecondary())
            setPadding(0, dp(2), 0, 0)
        })

        // ========== ОФОРМЛЕНИЕ ==========
        root.addView(sectionTitle(context, "ОФОРМЛЕНИЕ"))
        root.addView(switchRow(context, "Тёмная тема", !AppTheme.isLight) { checked ->
            AppTheme.isLight = !checked
            onThemeChanged()
            // Перезапускаем диалог для мгновенного применения цветов
            dialogRef?.dismiss()
            show(context, onThemeChanged, onResetAccount)
        })

        // ========== ГОЛОС ==========
        root.addView(sectionTitle(context, "ГОЛОСОВЫЕ УВЕДОМЛЕНИЯ"))
        root.addView(switchRow(context, "Озвучивать сигналы", RadarService.useVoice) { checked ->
            RadarService.useVoice = checked
        })

        // ========== АВТО-АДАПТ ==========
        root.addView(sectionTitle(context, "АВТО-АДАПТАЦИЯ СТРАТЕГИЙ"))
        root.addView(switchRow(context, "Авто-адапт включён", RadarService.autoAdaptEnabled) { checked ->
            RadarService.autoAdaptEnabled = checked
        })

        // ========== БЕЗОПАСНОСТЬ ==========
        root.addView(sectionTitle(context, "БЕЗОПАСНОСТЬ"))
        val killLabel = if (QuantEngine.killSwitchEngaged) "Kill Switch активен" else "Активировать Kill Switch"
        val killBtn = actionRow(context, killLabel, danger = true)
        killBtn.setOnClickListener {
            val turningOn = !QuantEngine.killSwitchEngaged
            QuantEngine.killSwitchEngaged = turningOn
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
            dialogRef?.dismiss()
        }
        root.addView(killBtn)

        // ========== СЧЁТ ==========
        root.addView(sectionTitle(context, "БУМАЖНЫЙ СЧЁТ"))
        val resetBtn = actionRow(context, "Сброс счёта", danger = true)
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

        // ========== ИНФОРМАЦИЯ ==========
        root.addView(sectionTitle(context, "ИНФОРМАЦИЯ"))
        val infoBtn = actionRow(context, "О приложении", danger = false)
        infoBtn.setOnClickListener { showAboutDialog(context) }
        root.addView(infoBtn)

        val helpBtn = actionRow(context, "Как пользоваться", danger = false)
        helpBtn.setOnClickListener { HelpDialog.show(context) }
        root.addView(helpBtn)

        // ========== FOOTER ==========
        val footer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(14), 0, 0)
        }
        footer.addView(TextView(context).apply {
            text = "Режим: BALANCED"
            textSize = 11f
            setTextColor(AppTheme.textTertiary())
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        footer.addView(TextView(context).apply {
            text = "Закрыть"
            textSize = 14f
            setTextColor(AppTheme.red())
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(dp(16), dp(6), 0, dp(6))
            setOnClickListener { dialogRef?.dismiss() }
        })
        root.addView(footer)

        dialogRef = AlertDialog.Builder(context).setView(scroll).create()
        dialogRef.show()
        dialogRef.window?.setBackgroundDrawable(UiHelpers.rounded(AppTheme.bgCard(), 22f))
    }

    private fun sectionTitle(context: Context, text: String): TextView {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        return TextView(context).apply {
            this.text = text
            textSize = 10f
            setTextColor(AppTheme.textTertiary())
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(dp(4), dp(8), 0, dp(4))
        }
    }

    private fun switchRow(
        context: Context,
        title: String,
        initialChecked: Boolean,
        onChecked: (Boolean) -> Unit
    ): LinearLayout {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val rowH = (ROW_HEIGHT_DP * density).toInt()

        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            background = UiHelpers.rounded(AppTheme.bgSubCard(), dp(12).toFloat())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                rowH
            ).apply { bottomMargin = dp(4) }

            addView(TextView(context).apply {
                this.text = title
                textSize = 14f
                setTextColor(AppTheme.textPrimary())
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })

            addView(Switch(context).apply {
                isChecked = initialChecked
                thumbTintList = ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    ),
                    intArrayOf(Color.WHITE, Color.WHITE)
                )
                trackTintList = ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    ),
                    intArrayOf(AppTheme.green(), Color.parseColor("#C7CED6"))
                )
                setOnCheckedChangeListener { _, checked -> onChecked(checked) }
            })
        }
    }

    private fun actionRow(context: Context, text: String, danger: Boolean): LinearLayout {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val rowH = (ROW_HEIGHT_DP * density).toInt()

        val bgDrawable = if (danger) {
            val red = AppTheme.red()
            val redLight = if (AppTheme.isLight) Color.parseColor("#FF6B70") else Color.parseColor("#FF7B80")
            GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(red, redLight)
            ).apply { cornerRadius = dp(12).toFloat() }
        } else {
            UiHelpers.rounded(AppTheme.bgSubCard(), dp(12).toFloat())
        }

        val textColor = if (danger) Color.WHITE else AppTheme.textPrimary()
        val arrowColor = if (danger) Color.WHITE else AppTheme.textTertiary()

        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), 0, dp(14), 0)
            background = bgDrawable
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                rowH
            ).apply { bottomMargin = dp(4) }
            isClickable = true
            isFocusable = true

            addView(TextView(context).apply {
                this.text = text
                textSize = 14f
                setTextColor(textColor)
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                maxLines = 1
            })

            addView(TextView(context).apply {
                this.text = "›"
                textSize = 20f
                setTextColor(arrowColor)
                gravity = Gravity.CENTER
            })
        }
    }

    private fun showAboutDialog(context: Context) {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        var dialogRef: AlertDialog? = null

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(12))
            setBackgroundColor(AppTheme.bgCard())
        }

        root.addView(TextView(context).apply {
            text = "О приложении"
            textSize = 19f
            setTextColor(AppTheme.textPrimary())
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        root.addView(TextView(context).apply {
            text = "PulseQuant — paper trading lab"
            textSize = 11f
            setTextColor(AppTheme.textSecondary())
            setPadding(0, dp(2), 0, dp(12))
        })

        root.addView(bodyText(context, "ЧТО ЭТО",
            "Бумажный тренажёр для мониторинга крипторынка и тестирования стратегий. Реальные ордера на биржу не отправляются. Данные — с публичного API BingX Swap."))

        root.addView(bodyText(context, "5 СТРАТЕГИЙ",
            "1. Patient Mean-Reversion — возврат к средней\n" +
            "2. Anti-Chase Momentum — умный пробой\n" +
            "3. Squeeze / Stop-Hunt — охота на сквизы\n" +
            "4. Patient Sniper — редкие точные входы\n" +
            "5. Whale-Flow — след крупных ордеров"))

        root.addView(bodyText(context, "6-БОТ DESK",
            "SCOUT — ищет сигналы\n" +
            "SKEPTIC — отклоняет рискованные входы\n" +
            "RISK — управляет размером позиции\n" +
            "COMPUTE — считает метрики\n" +
            "JOURNAL — следит за деградацией\n" +
            "PM — управляет капиталом"))

        root.addView(bodyText(context, "АРХИТЕКТУРА",
            "StrategyCore — единый движок сигналов для live и бэктеста.\n" +
            "QuantEngine — fair value, Z-score, дисбаланс, плечо.\n" +
            "BacktestEngine — история, OOS-тест, матрица параметров."))

        root.addView(bodyText(context, "ВАЖНО",
            "Backtest-результаты чувствительны к комиссиям, проскальзыванию и гранулярности свечей. Прошлая прибыль не гарантирует будущую."))

        val closeBtn = actionRow(context, "Понятно", danger = false)
        closeBtn.setOnClickListener { dialogRef?.dismiss() }
        closeBtn.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (ROW_HEIGHT_DP * density).toInt()
        ).apply { topMargin = dp(10) }
        root.addView(closeBtn)

        val scroll = ScrollView(context).apply { addView(root) }
        dialogRef = AlertDialog.Builder(context).setView(scroll).create()
        dialogRef.show()
        dialogRef.window?.setBackgroundDrawable(UiHelpers.rounded(AppTheme.bgCard(), 22f))
    }

    private fun bodyText(context: Context, title: String, text: String): LinearLayout {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = UiHelpers.rounded(AppTheme.bgSubCard(), dp(12).toFloat())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(6) }
        }
        col.addView(TextView(context).apply {
            this.text = title
            textSize = 10f
            setTextColor(AppTheme.textTertiary())
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, 0, 0, dp(4))
        })
        col.addView(TextView(context).apply {
            this.text = text
            textSize = 13f
            setTextColor(AppTheme.textPrimary())
            setLineSpacing(dp(2).toFloat(), 1.15f)
        })
        return col
    }
}
