package com.phone.cleaner.presentation.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.phone.cleaner.R

/**
 * The background every screen shares (Figma "Background", 1:4211): a flat fill with a lavender
 * glow in the top start corner and a blue glow on the top end edge.
 *
 * Both glows are elliptical radial gradients. A shape drawable can only draw a circular one, so
 * they are drawn here, stretched with a matrix. The geometry is the Figma gradient transform as a
 * fraction of the 375 x 812 frame, so it scales with the screen the way the design does. In right
 * to left the two glows swap sides, like the rest of the layout.
 *
 * Place it first in the screen's root, behind the system bars.
 */
class ScreenBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val fillColor = ContextCompat.getColor(context, R.color.screen_background)

    private val lavender = Glow(
        startColor = ContextCompat.getColor(context, R.color.screen_glow_lavender),
        endColor = ContextCompat.getColor(context, R.color.screen_glow_lavender_end),
        endStop = 0.6f,
        centerX = 45f / FRAME_WIDTH,
        centerY = 0f,
        radiusX = 450f / FRAME_WIDTH,
        radiusY = 730.8f / FRAME_HEIGHT,
    )

    private val blue = Glow(
        startColor = ContextCompat.getColor(context, R.color.screen_glow_blue),
        endColor = ContextCompat.getColor(context, R.color.screen_glow_blue_end),
        endStop = 0.62f,
        centerX = 1f,
        centerY = 64.96f / FRAME_HEIGHT,
        radiusX = 375f / FRAME_WIDTH,
        radiusY = 649.6f / FRAME_HEIGHT,
    )

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        lavender.layout(w, h, rtl)
        blue.layout(w, h, rtl)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(fillColor)
        lavender.draw(canvas, width, height)
        blue.draw(canvas, width, height)
    }

    private class Glow(
        startColor: Int,
        endColor: Int,
        endStop: Float,
        private val centerX: Float,
        private val centerY: Float,
        private val radiusX: Float,
        private val radiusY: Float,
    ) {
        private val shader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(startColor, endColor),
            floatArrayOf(0f, endStop),
            Shader.TileMode.CLAMP,
        )
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = this@Glow.shader }
        private val matrix = Matrix()

        fun layout(width: Int, height: Int, rtl: Boolean) {
            val x = if (rtl) 1f - centerX else centerX
            matrix.reset()
            matrix.setScale(radiusX * width, radiusY * height)
            matrix.postTranslate(x * width, centerY * height)
            shader.setLocalMatrix(matrix)
        }

        fun draw(canvas: Canvas, width: Int, height: Int) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }

    private companion object {
        const val FRAME_WIDTH = 375f
        const val FRAME_HEIGHT = 812f
    }
}
