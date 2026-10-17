package com.example.rhythmtrader.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

object HelpDialog {

    fun show(context: Context) {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        var dialogRef: AlertDialog? = null

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(12))
            setBackgroundColor(AppTheme.bgCard())
        }

        // Функция для создания серых плашек с заголовком и текстом
        fun addBlock(title: String, text: String) {
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
                textSize = 11f
                setTextColor(AppTheme.textSecondary())
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setPadding(0, 0, 0, dp(4))
            })
            
            col.addView(TextView(context).apply {
                this.text = text
                textSize = 13f
                setTextColor(AppTheme.textPrimary())
                setLineSpacing(dp(2).toFloat(), 1.15f)
            })
            
            root.addView(col)
        }

        // Вывод блоков без цифр
        addBlock("Вкладки", "«Радар» — основной экран: цена, график и сигналы.\n«Сделки» — открытые позиции и архив.")
        addBlock("Выбор монеты", "Нажми на BTC / ETH / SOL / ZEC / SUI, чтобы переключить пару. Долгое нажатие меняет монету.")
        addBlock("Запуск бота", "Большая кнопка «Запустить» включает торговый сервис.")
        addBlock("Стратегии", "Нажми на кнопку со стратегией, чтобы выбрать одну из 5.")
        addBlock("Авто-оптимизация", "Кнопка AUTO-OPTIMIZE запускает бэктест и выбирает лучшую стратегию.")
        addBlock("Ручной вход", "Кнопки LONG и SHORT открывают бумажную позицию вручную. «Закрыть» завершает сделку.")
        addBlock("Kill Switch", "Аварийная кнопка в настройках. Мгновенно закрывает все позиции.")
        addBlock("Настройки риска", "Ползунки TP/SL и «Институциональный ордер» влияют на чувствительность.")

        // Кнопка "Назад" (внутри скролла, в самом низу)
        val closeBtn = TextView(context).apply {
            text = "Назад"
            textSize = 14f
            setTextColor(AppTheme.textPrimary())
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            background = UiHelpers.rounded(AppTheme.bgSubCard(), dp(12).toFloat())
            setPadding(0, dp(14), 0, dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
            setOnClickListener { dialogRef?.dismiss() }
        }
        root.addView(closeBtn)

        val scroll = ScrollView(context).apply { addView(root) }
        dialogRef = AlertDialog.Builder(context).setView(scroll).create()
        dialogRef.show()
        dialogRef.window?.setBackgroundDrawable(UiHelpers.rounded(AppTheme.bgCard(), 22f))
    }
}
