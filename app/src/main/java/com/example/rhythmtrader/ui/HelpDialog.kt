package com.example.rhythmtrader.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView

object HelpDialog {

    fun show(context: Context) {
        val scroll = ScrollView(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 32, 36, 40)
        }
        scroll.addView(root)

        // Заголовок
        root.addView(TextView(context).apply {
            text = "Как пользоваться PulseQuant"
            textSize = 22f
            setTextColor(AppTheme.textPrimary())
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        })

        fun section(text: String) {
            root.addView(TextView(context).apply {
                this.text = text
                textSize = 13f
                setTextColor(AppTheme.accent())
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setPadding(0, 16, 0, 8)
            })
        }

        fun body(text: String) {
            root.addView(TextView(context).apply {
                this.text = text
                textSize = 14f
                setTextColor(AppTheme.textSecondary())
                setLineSpacing(0f, 1.25f)
                setPadding(0, 0, 0, 4)
            })
        }

        section("1. Кнопки сверху")
        body("⚙ — открыть Настройки (тема, голос, Kill Switch, сброс счёта).\n🌙 / ☀️ — быстрое переключение тёмной и светлой темы.")

        section("2. Вкладки")
        body("«Радар и График» — основной экран: цена, сигналы, управление ботом.\n«Сделки» — открытые позиции и архив закрытых сделок.")

        section("3. Выбор монеты")
        body("Нажми BTC / ETH / SOL / ZEC / SUI, чтобы переключить активную пару.\nДолгое нажатие — заменить монету.")

        section("4. Запуск бота")
        body("Большая красная кнопка «Остановить / Запустить 6-Bot Desk» включает и выключает торговый сервис.\nПока сервис выключен — новые сделки не открываются.")

        section("5. Стратегии")
        body("Синяя кнопка с названием — выбор одной из пяти стратегий:\n\n1. Patient Mean-Reversion — возврат к средней\n2. Anti-Chase Momentum — импульс без погони\n3. Squeeze / Stop-Hunt — отскок от экстремумов\n4. Patient Sniper — редкие точные входы\n5. Microstructure Whale-Flow — по крупным объёмам\n\nКнопка «i» рядом показывает подробности.")

        section("6. Авто-оптимизация")
        body("Фиолетовая кнопка «AUTO-OPTIMIZE» прогоняет все 5 стратегий по истории и выбирает лучшую по Out-of-Sample результатам.")

        section("7. Ручной вход")
        body("Зелёная LONG и красная SHORT — открыть бумажную позицию вручную по текущей цене.\n«Закрыть позицию» — закрыть текущую сделку по выбранной монете.")

        section("8. Kill Switch")
        body("Аварийная кнопка. Мгновенно запрещает новые входы и закрывает все открытые позиции.")

        section("9. Настройки риска")
        body("Ползунки «Ширина скобки» и «Институциональный ордер» влияют на чувствительность сигналов и размер «китовых» ордеров.")

        section("Важно")
        body("Это бумажный тренажёр. Реальные ордера на биржу не отправляются.\nВсегда смотри на режим (BALANCED / CHASE-ZONE / WILD-VOL) и оценку SKEPTIC.")

        val dialog = AlertDialog.Builder(context)
            .setView(scroll)
            .setNegativeButton("Закрыть", null)
            .create()

        dialog.show()
        dialog.window?.setBackgroundDrawable(UiHelpers.rounded(AppTheme.bgCard(), 28f))
    }
}
