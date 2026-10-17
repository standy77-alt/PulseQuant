package com.example.rhythmtrader.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

object BottomNavBar {
    fun create(context: Context, currentTab: Int, onTabSelected: (Int) -> Unit): LinearLayout {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        // Главный контейнер (вертикальный)
        val container = LinearLayout(context).apply {
            id = 9999
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(AppTheme.bgCard())
            elevation = dp(8).toFloat()
        }

        // 1. Тонкая линия сверху
        container.addView(View(context).apply {
            setBackgroundColor(AppTheme.stroke())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
        })

        // 2. Горизонтальный ряд с кнопками
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(16))
        }

        val tabs = listOf("Главная", "Аналитика", "Стратегии", "Профиль")

        for (i in tabs.indices) {
            val isSelected = i == currentTab
            val btn = TextView(context).apply {
                text = tabs[i]
                textSize = 12f
                gravity = Gravity.CENTER
                typeface = Typeface.create("sans-serif-medium", if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (isSelected) AppTheme.accent() else AppTheme.textSecondary())
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(0, dp(12), 0, dp(12))
                setOnClickListener { onTabSelected(i) }
            }
            row.addView(btn)
        }
        
        container.addView(row)
        return container
    }

    fun updateColors(context: Context, navBar: LinearLayout, currentTab: Int) {
        navBar.setBackgroundColor(AppTheme.bgCard())
        // Индекс 0 - линия, индекс 1 - ряд с кнопками
        val row = navBar.getChildAt(1) as? LinearLayout ?: return
        for (i in 0 until row.childCount) {
            val child = row.getChildAt(i) as TextView
            val isSelected = i == currentTab
            child.setTextColor(if (isSelected) AppTheme.accent() else AppTheme.textSecondary())
            child.typeface = Typeface.create("sans-serif-medium", if (isSelected) Typeface.BOLD else Typeface.NORMAL)
        }
    }
}
