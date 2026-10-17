package com.example.rhythmtrader.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
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
            setPadding(0, dp(10), 0, dp(20))
            setBackgroundColor(AppTheme.bgCard())

            // Пока используем эти 4 вкладки
            val tabs = listOf("🏠 Главная", "📊 Аналитика", "📋 Стратегии", "👤 Профиль")

            for (i in tabs.indices) {
                val isSelected = i == currentTab
                val btn = TextView(context).apply {
                    text = tabs[i]
                    textSize = 11f
                    gravity = Gravity.CENTER
                    typeface = Typeface.create("sans-serif-medium", if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                    setTextColor(if (isSelected) AppTheme.secondary() else AppTheme.textSecondary())
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener { onTabSelected(i) }
                }
                addView(btn)
            }
        }
    }

    fun updateColors(context: Context, navBar: LinearLayout, currentTab: Int) {
        navBar.setBackgroundColor(AppTheme.bgCard())
        for (i in 0 until navBar.childCount) {
            val child = navBar.getChildAt(i) as TextView
            val isSelected = i == currentTab
            child.setTextColor(if (isSelected) AppTheme.secondary() else AppTheme.textSecondary())
            child.typeface = Typeface.create("sans-serif-medium", if (isSelected) Typeface.BOLD else Typeface.NORMAL)
        }
    }
}
