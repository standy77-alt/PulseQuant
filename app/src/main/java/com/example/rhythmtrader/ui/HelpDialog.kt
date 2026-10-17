package com.example.rhythmtrader.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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

        // Основной контейнер (текст)
        val scroll = ScrollView(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(10))
        }
        scroll.addView(root)

        // Заголовок
        root.addView(TextView(context).apply {
            text = "Как пользоваться"
            textSize = 18f
            setTextColor(AppTheme.textPrimary())
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        })
        root.addView(TextView(context).apply {
            text = "Краткий гид по интерфейсу"
            textSize = 12f
            setTextColor(AppTheme.textSecondary())
            setPadding(0, dp(2), 0, dp(12))
        })

        fun section(text: String) {
            root.addView(TextView(context).apply {
                this.text = text
                textSize = 12f
                setTextColor(AppTheme.accent())
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setPadding(0, dp(12), 0, dp(4))
            })
        }

        fun body(text: String) {
            root.addView(TextView(context).apply {
                this.text = text
                textSize = 12f
                setTextColor(AppTheme.textSecondary())
                setLineSpacing(dp(2).toFloat(), 1.1f)
                setPadding(0, 0, 0, dp(4))
            })
        }

        section("1. Кнопки сверху")
        body("⚙ — Настройки. Иконка находится в правом верхнем углу.")
        section("2. Вкладки")
        body("«Радар» — основной экран: цена, график и сигналы.\n«Сделки» — открытые позиции и архив.")
        section("3. Выбор монеты")
        body("Нажми на BTC / ETH / SOL / ZEC / SUI, чтобы переключить пару. Долгое нажатие меняет монету.")
        section("4. Запуск бота")
        body("Большая кнопка «Запустить» включает торговый сервис.")
        section("5. Стратегии")
        body("Нажми на кнопку со стратегией, чтобы выбрать одну из 5.")
        section("6. Авто-оптимизация")
        body("Кнопка AUTO-OPTIMIZE запускает бэктест и выбирает лучшую стратегию.")
        section("7. Ручной вход")
        body("Кнопки LONG и SHORT открывают бумажную позицию вручную. «Закрыть» завершает сделку.")
        section("8. Kill Switch")
        body("Аварийная кнопка в настройках. Мгновенно закрывает все позиции.")
        section("9. Настройки риска")
        body("Ползунки TP/SL и «Институциональный ордер» влияют на чувствительность.")
        section("Важно")
        body("Это бумажный тренажёр. Реальные ордера на биржу не отправляются.")

        // Кастомная кнопка "Понятно"
        val closeBtn = TextView(context).apply {
            text = "Понятно"
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(AppTheme.red())
                cornerRadius = dp(12).toFloat()
            }
            setPadding(0, dp(14), 0, dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
            setOnClickListener { dialogRef?.dismiss() }
        }

        // Общий контейнер для диалога (текст + кнопка)
        val dialogLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(AppTheme.bgCard())
            // Даем скроллу занять всё доступное место, но не перекрывать кнопку
            addView(scroll, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            ))
            addView(closeBtn)
        }

        dialogRef = AlertDialog.Builder(context).setView(dialogLayout).create()
        dialogRef.show()
        dialogRef.window?.setBackgroundDrawable(UiHelpers.rounded(AppTheme.bgCard(), 22f))
    }
}
