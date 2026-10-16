package com.example.rhythmtrader.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.rhythmtrader.RadarService

object CoinSelectorDialog {

    fun show(
        context: Context,
        slotIndex: Int,
        currentSym: String,
        popularCoins: List<String>,
        isActive: Boolean,
        onSave: (String, Boolean) -> Unit
    ) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 40, 48, 24)
            setBackgroundColor(AppTheme.bgCard())
        }

        val titleView = TextView(context).apply {
            text = "Слот #${slotIndex + 1} ($currentSym)"
            textSize = 17f
            setTextColor(AppTheme.textPrimary())
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, 20)
        }
        container.addView(titleView)

        val input = EditText(context).apply {
            setText(currentSym.replace("-USDT", ""))
            hint = "Тикер (например CGPT, MON, DOGE)"
            setTextColor(AppTheme.textPrimary())
            setHintTextColor(AppTheme.textSecondary())
            background = UiHelpers.createAppleCard(AppTheme.bgSubCard(), 24f, AppTheme.stroke())
            setPadding(32, 24, 32, 24)
        }
        container.addView(input)

        val cbActive = CheckBox(context).apply {
            text = "Мониторить эту пару в фоне"
            isChecked = isActive
            setTextColor(AppTheme.accent())
            setPadding(0, 20, 0, 16)
        }
        container.addView(cbActive)

        // Кастомный список монет — цвета явно из AppTheme
        val listContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        var dialogRef: AlertDialog? = null

        for (sym in popularCoins) {
            val row = TextView(context).apply {
                text = sym
                textSize = 15f
                setTextColor(AppTheme.textPrimary())
                setPadding(24, 28, 24, 28)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 4 }
                setOnClickListener {
                    onSave(sym, true)
                    dialogRef?.dismiss()
                }
            }
            listContainer.addView(row)
        }

        val scrollList = ScrollView(context).apply {
            addView(listContainer)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                560
            )
        }
        container.addView(scrollList)

        dialogRef = AlertDialog.Builder(context)
            .setView(container)
            .setPositiveButton("Сохранить") { _, _ ->
                onSave(input.text.toString(), cbActive.isChecked)
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialogRef.show()

        // Красим кнопки диалога в акцент
        dialogRef.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(AppTheme.accent())
        dialogRef.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(AppTheme.textSecondary())

        dialogRef.window?.setBackgroundDrawable(
            UiHelpers.rounded(AppTheme.bgCard(), 28f)
        )
    }
}
