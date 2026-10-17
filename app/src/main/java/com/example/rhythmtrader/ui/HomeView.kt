package com.example.rhythmtrader.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.example.rhythmtrader.RadarService
import com.example.rhythmtrader.QuantEngine

object HomeView {

    // 1. НОВЫЙ БЛОК: Баланс (Тёмная карточка с графиком)
    fun createBalanceCard(context: Context): LinearLayout {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(16) }
            
            // Градиентный фон
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#1E293B"), Color.parseColor("#0F172A"))
            ).apply { cornerRadius = dp(20).toFloat() }

            addView(TextView(context).apply {
                text = "Баланс"
                textSize = 12f
                setTextColor(Color.parseColor("#94A3B8"))
            })
            
            val balanceRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.BOTTOM
                setPadding(0, dp(4), 0, dp(8))
            }
            balanceRow.addView(TextView(context).apply {
                text = String.format("$%.2f", RadarService.demoBalance)
                textSize = 28f
                setTextColor(Color.WHITE)
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            balanceRow.addView(TextView(context).apply {
                text = "+8.42% (24ч)"
                textSize = 12f
                setTextColor(Color.parseColor("#34C759"))
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            })
            addView(balanceRow)
        }
    }

    // 2. НОВЫЙ БЛОК: Кнопка запуска
    fun createStartButton(context: Context, isRunning: Boolean, onClick: () -> Unit): LinearLayout {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        return LinearLayout(context).apply {
            id = 7777
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(16))
            background = UiHelpers.rounded(if (isRunning) AppTheme.red() else AppTheme.accent(), dp(16).toFloat())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(20) }
            setOnClickListener { onClick() }

            addView(TextView(context).apply {
                text = if (isRunning) "⏹" else "▶"
                textSize = 18f
                setTextColor(Color.WHITE)
                setPadding(0, 0, dp(12), 0)
            })
            val col = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            col.addView(TextView(context).apply {
                text = if (isRunning) "Остановить" else "Запустить"
                textSize = 15f
                setTextColor(Color.WHITE)
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            col.addView(TextView(context).apply {
                text = if (isRunning) "Боты работают" else "20 тестов"
                textSize = 11f
                setTextColor(Color.parseColor("#B0C4DE"))
            })
            addView(col)
        }
    }

    // --- СУЩЕСТВУЮЩИЕ МЕТОДЫ (сетка ботов) ---
    fun createBotsGrid(context: Context): LinearLayout {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val container = LinearLayout(context).apply {
            id = 8888 
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(16) }
        }

        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), 0, dp(4), dp(12))
        }
        headerRow.addView(TextView(context).apply {
            text = "6 БОТОВ"
            textSize = 12f
            setTextColor(AppTheme.textSecondary())
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        headerRow.addView(TextView(context).apply {
            text = "Тестирование · 20 прогонов"
            textSize = 11f
            setTextColor(AppTheme.textTertiary())
        })
        container.addView(headerRow)

        val colors = listOf("#34C759", "#0A84FF", "#AF52DE", "#FF9F0A", "#00C7BE", "#FF453A")
        val names = listOf("Bot 1", "Bot 2", "Bot 3", "Bot 4", "Bot 5", "Bot 6")

        val row1 = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) } }
        for (i in 0..2) row1.addView(createBotCard(context, names[i], colors[i], true))
        container.addView(row1)

        val row2 = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) }
        for (i in 3..5) row2.addView(createBotCard(context, names[i], colors[i], true))
        container.addView(row2)

        return container
    }

    fun updateTheme(context: Context, container: LinearLayout?) {
        if (container == null) return
        container.setBackgroundColor(Color.TRANSPARENT)
        for (i in 1 until container.childCount) {
            val row = container.getChildAt(i) as? LinearLayout ?: continue
            for (j in 0 until row.childCount) {
                val card = row.getChildAt(j) as? LinearLayout ?: continue
                card.background = UiHelpers.rounded(AppTheme.bgCard(), (12 * context.resources.displayMetrics.density).toFloat(), AppTheme.stroke())
                val topRow = card.getChildAt(0) as? LinearLayout
                topRow?.getChildAt(1)?.let { (it as TextView).setTextColor(AppTheme.textPrimary()) }
                (card.getChildAt(1) as? TextView)?.setTextColor(AppTheme.textSecondary())
            }
        }
    }

    private fun createBotCard(context: Context, name: String, colorHex: String, isActive: Boolean): LinearLayout {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val color = Color.parseColor(colorHex)

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = UiHelpers.rounded(AppTheme.bgCard(), dp(12).toFloat(), AppTheme.stroke())
            setPadding(dp(10), dp(10), dp(10), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { if (name != "Bot 3" && name != "Bot 6") marginEnd = dp(8) }

            val topRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            topRow.addView(View(context).apply {
                background = UiHelpers.rounded(color, dp(4).toFloat())
                layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply { marginEnd = dp(6) }
            })
            topRow.addView(TextView(context).apply {
                text = name; textSize = 12f; setTextColor(AppTheme.textPrimary()); typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            })
            addView(topRow)

            addView(TextView(context).apply {
                text = if (isActive) "Активен" else "Ожидание"; textSize = 10f; setTextColor(AppTheme.textSecondary()); setPadding(0, dp(2), 0, dp(6))
            })

            addView(View(context).apply {
                background = UiHelpers.rounded(Color.argb(50, Color.red(color), Color.green(color), Color.blue(color)), dp(4).toFloat())
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(16))
            })
        }
    }
}

    fun updateStartButton(context: Context, container: LinearLayout?, isRunning: Boolean) {
        if (container == null) return
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        container.background = UiHelpers.rounded(if (isRunning) AppTheme.red() else AppTheme.accent(), dp(16).toFloat())
        
        (container.getChildAt(0) as? TextView)?.text = if (isRunning) "⏹" else "▶"
        val col = container.getChildAt(1) as? LinearLayout
        (col?.getChildAt(0) as? TextView)?.text = if (isRunning) "Остановить" else "Запустить"
        (col?.getChildAt(1) as? TextView)?.text = if (isRunning) "Боты работают" else "20 тестов"
    }
