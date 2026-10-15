package com.example.rhythmtrader

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

class RadarView(context: Context) : View(context) {

    data class TickPoint(val price: Double, val volumeUsdt: Double, val isBuy: Boolean)

    private val points = ArrayList<TickPoint>()
    private val maxPoints = 90
    var isLightTheme = false
        set(value) {
            field = value
            updateThemeColors()
            invalidate()
        }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val baseLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(12f, 10f), 0f)
    }

    private val upperTargetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9930D158")
        strokeWidth = 3f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
    }

    private val lowerTargetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#99FF453A")
        strokeWidth = 3f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
    }

    private val entryLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0A84FF")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val tpLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2E7D32")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val slLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C62828")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0A84FF")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val buyCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5530D158")
        style = Paint.Style.FILL
    }

    private val sellCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#55FF453A")
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    // УВЕЛИЧЕННЫЙ ЧЕТКИЙ ШРИФТ ДЛЯ TP / SL / БАЗЫ (30f вместо 21f)
    private val targetLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 30f
        color = Color.WHITE
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    init {
        updateThemeColors()
    }

    private fun updateThemeColors() {
        if (isLightTheme) {
            gridPaint.color = Color.parseColor("#E5E5EA")
            baseLinePaint.color = Color.parseColor("#883C3C43")
            labelPaint.color = Color.parseColor("#6C6C70")
        } else {
            gridPaint.color = Color.parseColor("#262629")
            baseLinePaint.color = Color.parseColor("#66EBEBF5")
            labelPaint.color = Color.parseColor("#8E8E93")
        }
    }

    fun addTick(price: Double, volumeUsdt: Double, isBuy: Boolean) {
        points.add(TickPoint(price, volumeUsdt, isBuy))
        if (points.size > maxPoints) points.removeAt(0)
        invalidate()
    }

    fun clearChart() {
        points.clear()
        invalidate()
    }

    private fun drawPriceTag(canvas: Canvas, text: String, x: Float, yCenter: Float, bgColor: Int) {
        val textW = targetLabelPaint.measureText(text)
        val padH = 16f
        val boxH = 44f
        val top = (yCenter - boxH / 2f).coerceIn(6f, height - boxH - 6f)
        val rect = RectF(x, top, x + textW + padH * 2, top + boxH)
        badgeBgPaint.color = bgColor
        canvas.drawRoundRect(rect, 12f, 12f, badgeBgPaint)
        targetLabelPaint.color = Color.WHITE
        canvas.drawText(text, x + padH, top + boxH - 11f, targetLabelPaint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val topPad = 42f
        val bottomPad = 42f
        val chartH = h - topPad - bottomPad

        canvas.drawLine(0f, topPad, w, topPad, gridPaint)
        canvas.drawLine(0f, topPad + chartH / 2f, w, topPad + chartH / 2f, gridPaint)
        canvas.drawLine(0f, h - bottomPad, w, h - bottomPad, gridPaint)

        val sym = RadarService.selectedChartSymbol
        if (points.size < 2) {
            canvas.drawText("Ожидание котировок ${sym.replace("-USDT", "")}...", 36f, h / 2f + 10f, labelPaint)
            return
        }

        val base = RadarService.basePrices[sym] ?: points.last().price
        val cfg = RadarService.getConfig(context, sym)
        val upperTrigger = base * (1.0 + cfg.thresholdPercent / 100.0)
        val lowerTrigger = base * (1.0 - cfg.thresholdPercent / 100.0)

        var minP = min(lowerTrigger, points.minOf { it.price })
        var maxP = max(upperTrigger, points.maxOf { it.price })

        val activePos = RadarService.openPositions[sym]
        if (activePos != null) {
            minP = min(minP, min(activePos.takeProfitPrice, activePos.stopLossPrice))
            maxP = max(maxP, max(activePos.takeProfitPrice, activePos.stopLossPrice))
        }

        val padRange = (maxP - minP) * 0.14
        minP -= padRange
        maxP += padRange
        val range = max(maxP - minP, minP * 0.0001)

        fun priceToY(p: Double): Float {
            val norm = ((p - minP) / range).toFloat()
            return h - bottomPad - norm * chartH
        }

        val lastPrice = points.last().price
        val isPositive = lastPrice >= base
        val mainColor = if (isPositive) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
        val gradTopColor = if (isPositive) Color.parseColor("#4430D158") else Color.parseColor("#44FF453A")
        linePaint.color = mainColor

        val path = Path()
        val fillPath = Path()
        val stepX = w / (maxPoints - 1).toFloat()

        var prevX = 0f
        var prevY = 0f

        for (i in points.indices) {
            val pt = points[i]
            val x = i * stepX
            val y = priceToY(pt.price)

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h - bottomPad)
                fillPath.lineTo(x, y)
            } else {
                val midX = (prevX + x) / 2f
                path.cubicTo(midX, prevY, midX, y, x, y)
                fillPath.cubicTo(midX, prevY, midX, y, x, y)
            }
            prevX = x
            prevY = y

            if (pt.volumeUsdt > 1500.0) {
                val radius = (ln(pt.volumeUsdt / 1000.0).toFloat() * 6.5f).coerceIn(5f, 36f)
                canvas.drawCircle(x, y, radius, if (pt.isBuy) buyCirclePaint else sellCirclePaint)
            }
        }

        fillPath.lineTo(prevX, h - bottomPad)
        fillPath.close()

        fillPaint.shader = LinearGradient(0f, topPad, 0f, h - bottomPad, gradTopColor, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)

        // Рисуем линии уровней и крупные контрастные капсулы с цифрами поверх графика
        if (activePos != null) {
            val yEntry = priceToY(activePos.entryPrice)
            val yTp = priceToY(activePos.takeProfitPrice)
            val ySl = priceToY(activePos.stopLossPrice)

            canvas.drawLine(0f, yTp, w, yTp, tpLinePaint)
            canvas.drawLine(0f, yEntry, w, yEntry, entryLinePaint)
            canvas.drawLine(0f, ySl, w, ySl, slLinePaint)

            val sideTag = if (activePos.isLong) "LONG" else "SHORT"
            drawPriceTag(canvas, "TP: ${formatPrice(activePos.takeProfitPrice)}", 16f, yTp, Color.parseColor("#DD1B5E20"))
            drawPriceTag(canvas, "$sideTag: ${formatPrice(activePos.entryPrice)}", w - 310f, yEntry, Color.parseColor("#DD0D47A1"))
            drawPriceTag(canvas, "SL: ${formatPrice(activePos.stopLossPrice)}", 16f, ySl, Color.parseColor("#DDB71C1C"))
        } else {
            val yUpper = priceToY(upperTrigger)
            val yBase = priceToY(base)
            val yLower = priceToY(lowerTrigger)

            canvas.drawLine(0f, yUpper, w, yUpper, upperTargetPaint)
            canvas.drawLine(0f, yBase, w, yBase, baseLinePaint)
            canvas.drawLine(0f, yLower, w, yLower, lowerTargetPaint)

            drawPriceTag(canvas, "▲ TP/ВХОД ${formatPrice(upperTrigger)}", 16f, yUpper, Color.parseColor("#CC2E7D32"))
            drawPriceTag(canvas, "БАЗА ${formatPrice(base)}", w - 280f, yBase, Color.parseColor("#CC48484A"))
            drawPriceTag(canvas, "▼ SL/ВХОД ${formatPrice(lowerTrigger)}", 16f, yLower, Color.parseColor("#CCC62828"))
        }

        canvas.drawCircle(prevX, prevY, 9f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLightTheme) Color.BLACK else Color.WHITE
        })
        canvas.drawCircle(prevX, prevY, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = mainColor })
    }

    private fun formatPrice(p: Double): String {
        return when {
            p >= 1000 -> String.format("%.1f", p)
            p >= 10 -> String.format("%.2f", p)
            else -> String.format("%.4f", p)
        }
    }
}
