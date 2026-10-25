package com.phone.cleaner.presentation.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.phone.cleaner.R

/**
 * The scanning ring (Figma 1:508): a 30% blue track and a blue arc with round ends, starting at
 * the top and running clockwise as far as [progress] has got. The design draws a fixed quarter arc
 * (a loading icon); here the arc is the scan's real progress.
 *
 * The geometry is the Figma frame's: in a 205 px square the ring's centre line has a 76.9 px
 * radius and the stroke is 17.1 px wide, so both scale with the view.
 */
class ProgressRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    /** 0..1. */
    var progress: Float = 0f
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            if (field == clamped) return
            field = clamped
            invalidate()
        }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.progress_ring_track)
    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.progress_ring)
    }

    private val oval = RectF()

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = minOf(w, h).toFloat()
        val stroke = size * STROKE / FRAME
        val radius = size * RADIUS / FRAME
        trackPaint.strokeWidth = stroke
        arcPaint.strokeWidth = stroke
        oval.set(w / 2f - radius, h / 2f - radius, w / 2f + radius, h / 2f + radius)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawOval(oval, trackPaint)
        // Clockwise from the top; in right to left it runs the other way, like the layout.
        val sweep = 360f * progress
        if (sweep > 0f) {
            val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
            canvas.drawArc(oval, -90f, if (rtl) -sweep else sweep, false, arcPaint)
        }
    }

    private companion object {
        const val FRAME = 205f
        const val RADIUS = 76.9f
        const val STROKE = 17.08f
    }
}
