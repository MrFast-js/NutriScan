package com.example.nutritionalbarcodescanner

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class CircularProgressBar(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var progress = 0f
    private val maxProgress = 100f

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#d1d1d1")
        style = Paint.Style.STROKE
        strokeWidth = 40f // Adjust the stroke width as needed
        strokeCap = Paint.Cap.ROUND // Make the edges rounded
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 40f // Adjust the stroke width as needed
        strokeCap = Paint.Cap.ROUND // Make the edges rounded
    }

    private val oval = RectF()

    private val animator = ValueAnimator().apply {
        interpolator = AccelerateDecelerateInterpolator()
        duration = 1000 // Animation duration in milliseconds
    }

    init {
        animator.addUpdateListener { animation ->
            progress = animation.animatedValue as Float
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (Math.min(width, height) - circlePaint.strokeWidth) / 2f

        progressPaint.color = transitionColor(progress / maxProgress)

        // Set the progress arc
        oval.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        // Draw the progress arc
        canvas.drawArc(oval, 135f, 270f, false, circlePaint)

        // Set the progress arc
        oval.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

        // Draw the progress arc
        canvas.drawArc(oval, 135f, progress / maxProgress * 270f, false, progressPaint)
    }

    fun transitionColor(percent: Float): Int {
        val startColor = Color.parseColor("#b02815")
        val endColor = Color.parseColor("#24cf1b")

        val hsvStart = FloatArray(3)
        Color.colorToHSV(startColor, hsvStart)

        val hsvEnd = FloatArray(3)
        Color.colorToHSV(endColor, hsvEnd)

        val hue = interpolate(hsvStart[0], hsvEnd[0], percent)
        val saturation = interpolate(hsvStart[1], hsvEnd[1], percent)
        val value = interpolate(hsvStart[2], hsvEnd[2], percent)

        return Color.HSVToColor(floatArrayOf(hue, saturation, value))
    }

    fun interpolate(start: Float, end: Float, percent: Float): Float {
        return start + (end - start) * percent
    }

    fun setProgress(newProgress: Float) {
        animator.setFloatValues(progress, newProgress)
        animator.start()
    }
}