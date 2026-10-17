package com.example.rhythmtrader.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.rhythmtrader.QuantEngine
import com.example.rhythmtrader.RadarService

object StrategySelectorDialog {

    private data class StratInfo(
        val mode: Int,
        val name: String,
        val simpleRule: String,
        val howItWorks: String,
        val colorHex: String
    )

    fun show(context: Context, onUpdated: () -> Unit) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(44, 40, 44, 28)
            setBackgroundColor(AppTheme.bgCard())
        }

        val title = TextView(context).apply {
            text = "Выбор стратегии (5 Постулатов DXRG)"
            textSize = 18f
            setTextColor(AppTheme.textPrimary())
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, 0, 0, 10)
        }
        container.addView(title)

        val sub = TextView(context).apply {
            text = "Нажми на любую карточку ниже, чтобы переключить алгоритм бота:"
            textSize = 13f
            setTextColor(AppTheme.textSecondary())
            setPadding(0, 0, 0, 24)
        }
        container.addView(sub)

        var dialogRef: AlertDialog? = null

        val strategies = listOf(
            StratInfo(0, "1. Patient Mean-Reversion (Возврат к норме)",
                "«Покупай, когда все в панике сливают, и продавай, когда толпа жадно скупает».",
                "Ждет сильного отклонения цены от средней справедливой стоимости (VWAP), как натянутой резинки. Когда паника в стакане стихает — открывает сделку на возврат к среднему.",
                "#2E7D32"),
            StratInfo(1, "2. Anti-Chase Momentum (Умный пробой)",
                "«Прыгай в ракету только на старте, но никогда не догоняй уходящий поезд».",
                "Идет по тренду вместе с давлением крупных игроков (CVD). Тормоз Chase Veto блокирует покупку, если свеча улетела дальше 0.75%.",
                "#0A84FF"),
            StratInfo(2, "3. Squeeze / Stop-Hunt (Охота на сквизы)",
                "«Заходи в момент, когда маркет-мейкер сбил чужие стопы и резко выкупает цену обратно».",
                "Бот ждет прокола локального дна или вершины и мгновенного обратного выкупа крупным объемом.",
                "#FF9500"),
            StratInfo(3, "4. Core / Patient Sniper (Терпеливый снайпер)",
                "«Торгуй редко, но метко — только в идеальных условиях и с широкой целью».",
                "Включается на спокойной волатильности (S1–S4) при перевесе стакана >45%.",
                "#AF52DE"),
            StratInfo(4, "5. Microstructure Whale-Flow (След Китов)",
                "«Не смотри на мелкий шум — открывай сделку сразу за крупным рыночным ордером кита».",
                "Фиксирует удар крупного объема по рынку и садится на хвост крупному игроку.",
                "#00C7BE")
        )

        for (st in strategies) {
            val isSelected = RadarService.botStrategyMode == st.mode
            val accentColor = Color.parseColor(st.colorHex)

            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = UiHelpers.createAppleCard(
                    AppTheme.bgSubCard(), 32f,
                    if (isSelected) accentColor else AppTheme.stroke()
                )
                setPadding(32, 26, 32, 26)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 18 }
                setOnClickListener {
                    RadarService.botStrategyMode = st.mode
                    RadarService.saveBotStats(context)
                    onUpdated()
                    dialogRef?.dismiss()
                    Toast.makeText(context, "Активирован: ${st.name}", Toast.LENGTH_SHORT).show()
                }
            }

            val badge = if (isSelected) "  ✓ АКТИВНА" else ""
            val tvName = TextView(context).apply {
                text = "${st.name}$badge"
                textSize = 15f
                setTextColor(if (isSelected) accentColor else AppTheme.textPrimary())
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                setPadding(0, 0, 0, 8)
            }

            val tvRule = TextView(context).apply {
                text = "Простыми словами: ${st.simpleRule}"
                textSize = 13f
                setTextColor(AppTheme.textPrimary())
                typeface = Typeface.create("sans-serif-medium", Typeface.ITALIC)
                setPadding(0, 0, 0, 8)
            }

            val tvHow = TextView(context).apply {
                text = st.howItWorks
                textSize = 12f
                setTextColor(AppTheme.textSecondary())
                setLineSpacing(5f, 1f)
            }

            card.addView(tvName)
            card.addView(tvRule)
            card.addView(tvHow)
            container.addView(card)
        }

        val scrollDialog = ScrollView(context).apply { addView(container) }
        dialogRef = AlertDialog.Builder(context)
            .setView(scrollDialog)
            .setNegativeButton("Закрыть", null)
            .create()
        dialogRef.show()

        dialogRef.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(AppTheme.textSecondary())
        dialogRef.window?.setBackgroundDrawable(UiHelpers.rounded(AppTheme.bgCard(), 28f))
    }
}
