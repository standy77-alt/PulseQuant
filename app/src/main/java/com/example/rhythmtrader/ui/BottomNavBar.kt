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

        return LinearLayout(context).apply {
            id = 9999 // ID для поиска при смене темы
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(16))
            setBackgroundColor(AppTheme.bgCard())
            elevation = dp(8).toFloat() // Легкая тень, чтобы отделить от контента

            // Тонкая линия сверху
            addView(View(context).apply {
                setBackgroundColor(AppTheme.stroke())
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
            })

            // Текст без эмодзи
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
                addView(btn)
            }
        }
    }

    fun updateColors(context: Context, navBar: LinearLayout, currentTab: Int) {
        navBar.setBackgroundColor(AppTheme.bgCard())
        // Индекс 0 - это разделительная линия, поэтому кнопки начинаются с 1
        for (i in 1 until navBar.childCount) {
            val child = navBar.getChildAt(i) as TextView
            val tabIndex = i - 1
            val isSelected = tabIndex == currentTab
            child.setTextColor(if (isSelected) AppTheme.accent() else AppTheme.textSecondary())
            child.typeface = Typeface.create("sans-serif-medium", if (isSelected) Typeface.BOLD else Typeface.NORMAL)
        }
    }
}
