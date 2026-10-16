package com.example.rhythmtrader.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

object UiHelpers {

    fun rounded(color: Int, radius: Float, strokeColor: Int? = null, strokeWidth: Int = 2): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            if (strokeColor != null) setStroke(strokeWidth, strokeColor)
        }
    }

    fun createCard(context: Context, radius: Float = 28f): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(AppTheme.bgCard(), radius, AppTheme.stroke())
            setPadding(32, 28, 32, 28)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }
    }

    fun createTitle(context: Context, text: String, size: Float = 16f): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = size
            setTextColor(AppTheme.textPrimary())
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
    }

    fun createSubtitle(context: Context, text: String, size: Float = 13f): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = size
            setTextColor(AppTheme.textSecondary())
        }
    }

    fun createPrimaryButton(context: Context, text: String): Button {
        return Button(context).apply {
            this.text = text
            textSize = 15f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(AppTheme.accent(), 18f)
            stateListAnimator = null
            setPadding(24, 22, 24, 22)
        }
    }

    fun createSecondaryButton(context: Context, text: String): Button {
        return Button(context).apply {
            this.text = text
            textSize = 14f
            isAllCaps = false
            setTextColor(AppTheme.textPrimary())
            background = rounded(AppTheme.btnSecondaryBg(), 18f, AppTheme.stroke())
            stateListAnimator = null
            setPadding(24, 20, 24, 20)
        }
    }

    fun createDangerButton(context: Context, text: String): Button {
        return Button(context).apply {
            this.text = text
            textSize = 14f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(AppTheme.btnDangerBg(), 18f)
            stateListAnimator = null
            setPadding(24, 20, 24, 20)
        }
    }
}
