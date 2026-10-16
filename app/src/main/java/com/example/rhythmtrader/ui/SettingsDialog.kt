package com.example.rhythmtrader.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.example.rhythmtrader.QuantEngine
import com.example.rhythmtrader.RadarService

object SettingsDialog {

    fun show(context: Context, onThemeChanged: () -> Unit, onResetAccount: () -> Unit) {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        var dialogRef: AlertDialog? = null

        val scroll = ScrollView(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(16))
            setBackgroundColor(AppTheme.bgCard())
        }
        scroll.addView(root)

        // ========== HEADER ==========
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(20))
        }
        header.addView(iconCircle(context, "⚙", dp(48)))

        val titleBlock = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(10), 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleBlock.addView(TextView(context).apply {
            text = "Настройки"
            textSize = 22f
            setTextColor(AppTheme.textPrimary())
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        })
        titleBlock.addView(TextView(context).apply {
            text = "Персонализация и управление"
            textSize = 13f
            setTextColor(AppTheme.textSecondary())
            setPadding(0, dp(2), 0, 0)
        })
        header.addView(titleBlock)

        header.addView(iconCircle(context, "✕", dp(40)) { dialogRef?.dismiss() })
        root.addView(header)

        // ========== ОФОРМЛЕНИЕ ==========
        root.addView(sectionTitle(context, "ОФОРМЛЕНИЕ"))
        root.addView(switchRow(
            context, "🌙", "Тёмная тема",
            "Автоматическая адаптация интерфейса",
            !AppTheme.isLight
        ) { checked ->
            AppTheme.isLight = !checked
            onThemeChanged()
        })
        root.addView(divider(context))

        // ========== ГОЛОСОВЫЕ УВЕДОМЛЕНИЯ ==========
        root.addView(sectionTitle(context, "ГОЛОСОВЫЕ УВЕДОМЛЕНИЯ"))
        root.addView(switchRow(
            context, "🔊", "Озвучивать сигналы",
            "Голосовые уведомления о событиях",
            RadarService.useVoice
        ) { checked ->
            RadarService.useVoice = checked
        })
        root.addView(divider(context))

        // ========== АВТО-АДАПТАЦИЯ ==========
        root.addView(sectionTitle(context, "АВТО-АДАПТАЦИЯ СТРАТЕГИЙ"))
        root.addView(switchRow(
            context, "⚡", "Авто-адапт включён",
            "Система подстраивает стратегии под рынок",
            RadarService.autoAdaptEnabled
        ) { checked ->
            RadarService.autoAdaptEnabled = checked
        })
        root.addView(divider(context))

        // ========== БЕЗОПАСНОСТЬ ==========
        root.addView(sectionTitle(context, "БЕЗОПАСНОСТЬ"))
        val killLabel = if (QuantEngine.killSwitchEngaged) "Kill Switch активен" else "Активировать Kill Switch"
        val killBtn = dangerRow(context, "🛡", killLabel)
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

        // ========== БУМАЖНЫЙ СЧЁТ ==========
        root.addView(sectionTitle(context, "БУМАЖНЫЙ СЧЁТ"))
        val resetBtn = dangerRow(context, "📄", "Сбросить бумажный счёт")
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
        root.addView(infoRow(
            context, "ℹ",
            "PulseQuant — paper trading lab",
            "Стратегии используют shared StrategyCore + QuantEngine"
        ))
        val helpRow = infoRow(context, "?", "Как пользоваться", null)
        helpRow.setOnClickListener { HelpDialog.show(context) }
        root.addView(helpRow)

        // ========== FOOTER ==========
        val footer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(24), 0, 0)
        }
        footer.addView(TextView(context).apply {
            text = "📊  Режим: BALANCED"
            textSize = 12f
            setTextColor(AppTheme.textTertiary())
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        footer.addView(TextView(context).apply {
            text = "Закрыть"
            textSize = 15f
            setTextColor(AppTheme.red())
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(dp(16), dp(8), 0, dp(8))
            setOnClickListener { dialogRef?.dismiss() }
        })
        root.addView(footer)

        dialogRef = AlertDialog.Builder(context).setView(scroll).create()
        dialogRef.show()
        dialogRef.window?.setBackgroundDrawable(UiHelpers.rounded(AppTheme.bgCard(), 24f))
    }

    // ==========================================
    // Хелперы
    // ==========================================

    private fun iconCircle(
        context: Context,
        emoji: String,
        size: Int,
        onClick: (() -> Unit)? = null
    ): TextView {
        return TextView(context).apply {
            text = emoji
            textSize = size / 2.4f
            gravity = Gravity.CENTER
            setTextColor(AppTheme.textPrimary())
            background = UiHelpers.rounded(AppTheme.bgSubCard(), size.toFloat() / 2f)
            layoutParams = LinearLayout.LayoutParams(size, size)
            if (onClick != null) {
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick() }
            }
        }
    }

    private fun sectionTitle(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 11f
            setTextColor(AppTheme.textTertiary())
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, 20, 0, 12)
        }
    }

    private fun divider(context: Context): View {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        return View(context).apply {
            setBackgroundColor(AppTheme.stroke())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
            ).apply { topMargin = dp(4) }
        }
    }

    private fun switchRow(
        context: Context,
        icon: String,
        title: String,
        subtitle: String,
        initialChecked: Boolean,
        onChecked: (Boolean) -> Unit
    ): LinearLayout {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(12))

            addView(iconCircle(context, icon, dp(40)))

            val textCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), 0, dp(10), 0)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(context).apply {
                text = title
                textSize = 15f
                setTextColor(AppTheme.textPrimary())
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            })
            textCol.addView(TextView(context).apply {
                text = subtitle
                textSize = 12f
                setTextColor(AppTheme.textSecondary())
                setPadding(0, dp(2), 0, 0)
            })
            addView(textCol)

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
                    intArrayOf(AppTheme.green(), AppTheme.bgSubCard())
                )
                setOnCheckedChangeListener { _, checked -> onChecked(checked) }
            })
        }
    }

    private fun dangerRow(context: Context, icon: String, text: String): LinearLayout {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val red = AppTheme.red()
        val redLight = if (AppTheme.isLight) Color.parseColor("#FF6B70") else Color.parseColor("#FF7B80")

        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(red, redLight)
            ).apply {
                cornerRadius = dp(14).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4) }

            addView(TextView(context).apply {
                this.text = icon
                textSize = 18f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = UiHelpers.rounded(Color.parseColor("#33FFFFFF"), dp(20).toFloat())
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            })

            addView(TextView(context).apply {
                this.text = text
                textSize = 15f
                setTextColor(Color.WHITE)
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setPadding(dp(14), 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })

            addView(TextView(context).apply {
                this.text = "›"
                textSize = 24f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            })
        }
    }

    private fun infoRow(context: Context, icon: String, title: String, subtitle: String?): LinearLayout {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = UiHelpers.rounded(AppTheme.bgSubCard(), dp(14).toFloat())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(4)
                bottomMargin = dp(4)
            }
            isClickable = true
            isFocusable = true

            addView(TextView(context).apply {
                this.text = icon
                textSize = 18f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = UiHelpers.rounded(AppTheme.secondary(), dp(20).toFloat())
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            })

            val textCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), 0, dp(10), 0)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(context).apply {
                text = title
                textSize = 14f
                setTextColor(AppTheme.textPrimary())
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            })
            if (!subtitle.isNullOrEmpty()) {
                textCol.addView(TextView(context).apply {
                    text = subtitle
                    textSize = 12f
                    setTextColor(AppTheme.textSecondary())
                    setPadding(0, dp(2), 0, 0)
                })
            }
            addView(textCol)

            addView(TextView(context).apply {
                this.text = "›"
                textSize = 24f
                setTextColor(AppTheme.textTertiary())
                gravity = Gravity.CENTER
            })
        }
    }
}
