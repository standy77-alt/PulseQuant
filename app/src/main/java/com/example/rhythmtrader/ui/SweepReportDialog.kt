package com.example.rhythmtrader.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.rhythmtrader.BacktestEngine
import com.example.rhythmtrader.RadarService

object SweepReportDialog {

    fun show(
        context: Context,
        report: BacktestEngine.SweepReport,
        onCoinSelected: (String) -> Unit
    ) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(44, 40, 44, 28)
            setBackgroundColor(AppTheme.bgCard())
        }

        val title = TextView(context).apply {
            text = "Compute Lab • ${report.symbol}"
            textSize = 19f
            setTextColor(AppTheme.textPrimary())
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, 0, 0, 6)
        }
        container.addView(title)

        val sub = TextView(context).apply {
            text = "${report.strategyName}\nПроверено 20 конфигураций на ${report.candlesCount} свечах (5m) с вычетом 11 bps комиссий:"
            textSize = 12f
            setTextColor(AppTheme.textSecondary())
            setPadding(0, 0, 0, 20)
        }
        container.addView(sub)

        val best = report.bestCell
        val verdictColor = when (best.verdict) {
            "DATA VALIDATED" -> Color.parseColor("#2E7D32")
            "TRADE-OFF" -> Color.parseColor("#EF6C00")
            else -> Color.parseColor("#C62828")
        }

        val verdictBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = UiHelpers.createAppleCard(verdictColor, 28f, Color.TRANSPARENT)
            setPadding(32, 24, 32, 24)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 18 }
        }
        val tvVerdictTitle = TextView(context).apply {
            text = "ВЕРДИКТ: ${best.verdict}"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        val plateauText = if (report.isPlateau) {
            "✓ ПЛАТО ПОДТВЕРЖДЕНО: Соседние настройки тоже в плюсе (устойчивое преимущество)."
        } else {
            "⚠️ УЗКИЙ ПИК: Соседние настройки слабее, будь осторожен с переобучением."
        }
        val tvVerdictSub = TextView(context).apply {
            text = "Лучший порог: ${String.format("%.2f%%", best.thresholdPct)} (TP/SL -${best.slBps.toInt()}/+${best.tpBps.toInt()} bps)\n" +
                   "Train: ${String.format("%+.0f", best.trainNetBps)} bps (Sharpe ${String.format("%.2f", best.trainSharpe)}) | " +
                   "Out-of-Sample: ${String.format("%+.0f", best.oosNetBps)} bps (Sharpe ${String.format("%.2f", best.oosSharpe)})\n" +
                   "$plateauText"
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(0, 8, 0, 0)
            setLineSpacing(5f, 1f)
        }
        verdictBox.addView(tvVerdictTitle)
        verdictBox.addView(tvVerdictSub)
        container.addView(verdictBox)

        val matrixLabel = TextView(context).apply {
            text = "ТОП-5 КОНФИГУРАЦИЙ ИЗ МАТРИЦЫ СВИПА:"
            textSize = 12f
            setTextColor(AppTheme.accent())
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, 6, 0, 10)
        }
        container.addView(matrixLabel)

        for (cell in report.topCells) {
            val row = TextView(context).apply {
                val badge = when (cell.verdict) {
                    "DATA VALIDATED" -> "🟢 VALIDATED"
                    "TRADE-OFF" -> "🟡 TRADE-OFF"
                    else -> "🔴 NO EDGE"
                }
                text = "$badge • Порог ${String.format("%.2f%%", cell.thresholdPct)} (-${cell.slBps.toInt()}/+${cell.tpBps.toInt()} bps)\n" +
                       "Train: ${String.format("%+.0f", cell.trainNetBps)} bps | OOS: ${String.format("%+.0f", cell.oosNetBps)} bps | Сделок: ${cell.totalTrades} (WR: ${String.format("%.0f%%", cell.winRatePct)})"
                textSize = 12f
                setTextColor(AppTheme.textPrimary())
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                background = UiHelpers.createAppleCard(AppTheme.bgSubCard(), 22f, AppTheme.stroke())
                setPadding(24, 16, 24, 16)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 10 }
            }
            container.addView(row)
        }

        val futureBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = UiHelpers.createAppleCard(AppTheme.bgSubCard(), 26f, AppTheme.accent())
            setPadding(28, 22, 28, 22)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 10
                bottomMargin = 18
            }
        }
        val ftTitle = TextView(context).apply {
            text = "🛡️ FUTURETEST (Стресс-тест шоковых событий)"
            textSize = 13f
            setTextColor(AppTheme.accent())
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, 0, 0, 6)
        }
        val ftBody = TextView(context).apply {
            text = "• Сценарий 1 (Flash Crash -5% за минуту): Защитный TP/SL отсечет левый хвост, риск ограничен ${String.format("%+.2f$", report.flashCrash5PctImpactUsdt)}.\n" +
                   "• Сценарий 2 (Каскад ликвидаций -12% с проскальзыванием): Максимальный удар по позиции составит ${String.format("%+.2f$", report.cascade12PctImpactUsdt)} (ликвидация предотвращена)."
            textSize = 12f
            setTextColor(AppTheme.textSecondary())
            setLineSpacing(4f, 1f)
        }
        futureBox.addView(ftTitle)
        futureBox.addView(ftBody)
        container.addView(futureBox)

        var dialogRef: AlertDialog? = null

        val btnApply = Button(context).apply {
            text = "✅ Применить порог ${String.format("%.2f%%", best.thresholdPct)}"
            textSize = 14f
            isAllCaps = false
            minHeight = 0
            minimumHeight = 0
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            background = UiHelpers.createAppleCard(Color.parseColor("#2E7D32"), 26f, Color.TRANSPARENT)
            setPadding(24, 26, 24, 26)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
            setOnClickListener {
                val curCfg = RadarService.getConfig(context, report.symbol)
                RadarService.saveConfig(context, report.symbol, best.thresholdPct, curCfg.whaleUsdt)
                onCoinSelected(report.symbol)
                dialogRef?.dismiss()
                Toast.makeText(context, "Применен порог ${String.format("%.2f%%", best.thresholdPct)} для ${report.symbol}", Toast.LENGTH_SHORT).show()
            }
        }
        container.addView(btnApply)

        val btnCloseModal = Button(context).apply {
            text = "Закрыть окно"
            textSize = 13f
            isAllCaps = false
            minHeight = 0
            minimumHeight = 0
            setTextColor(AppTheme.textPrimary())
            background = UiHelpers.createAppleCard(AppTheme.bgSubCard(), 26f, AppTheme.stroke())
            setPadding(24, 22, 24, 22)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { dialogRef?.dismiss() }
        }
        container.addView(btnCloseModal)

        val scrollDialog = ScrollView(context).apply { addView(container) }
        dialogRef = AlertDialog.Builder(context).setView(scrollDialog).create()
        dialogRef.show()

        dialogRef.window?.setBackgroundDrawable(UiHelpers.rounded(AppTheme.bgCard(), 28f))
    }
}
