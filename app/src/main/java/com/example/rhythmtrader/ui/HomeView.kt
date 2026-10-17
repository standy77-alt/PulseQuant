package com.example.rhythmtrader.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

object HomeView {

    fun createBotsGrid(context: Context): LinearLayout {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }
        }

        // Заголовок "6 БОТОВ"
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

        // Сетка (2 ряда по 3 бота)
        val colors = listOf("#34C759", "#0A84FF", "#AF52DE", "#FF9F0A", "#00C7BE", "#FF453A")
        val names = listOf("Bot 1", "Bot 2", "Bot 3", "Bot 4", "Bot 5", "Bot 6")

        // Ряд 1
        val row1 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) }
        }
        for (i in 0..2) {
            row1.addView(createBotCard(context, names[i], colors[i], true))
        }
        container.addView(row1)

        // Ряд 2
        val row2 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        for (i in 3..5) {
            row2.addView(createBotCard(context, names[i], colors[i], true))
        }
        container.addView(row2)

        return container
    }

    private fun createBotCard(context: Context, name: String, colorHex: String, isActive: Boolean): LinearLayout {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val color = Color.parseColor(colorHex)

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = UiHelpers.rounded(AppTheme.bgCard(), dp(12).toFloat(), AppTheme.stroke())
            setPadding(dp(10), dp(10), dp(10), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { 
                if (name != "Bot 3" && name != "Bot 6") marginEnd = dp(8) 
            }

            // Верхняя строка: Точка + Имя
            val topRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            topRow.addView(View(context).apply {
                background = UiHelpers.rounded(color, dp(4).toFloat())
                layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply { marginEnd = dp(6) }
            })
            topRow.addView(TextView(context).apply {
                text = name
                textSize = 12f
                setTextColor(AppTheme.textPrimary())
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            })
            addView(topRow)

            // Статус
            addView(TextView(context).apply {
                text = if (isActive) "Активен" else "Ожидание"
                textSize = 10f
                setTextColor(AppTheme.textSecondary())
                setPadding(0, dp(2), 0, dp(6))
            })

            // Имитация мини-графика (просто цветная полоска внизу)
            addView(View(context).apply {
                background = UiHelpers.rounded(color.copy(alpha = 50), dp(4).toFloat())
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(16))
            })
        }
    }
}
