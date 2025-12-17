package com.example.aircare

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class AnimatedBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs) {

    private val paint = Paint()
    private val bubbles = mutableListOf<Bubble>()
    private val colors = listOf(
        Color.argb(50, 173, 216, 230), // Light Blue
        Color.argb(50, 144, 238, 144), // Light Green
        Color.argb(50, 240, 248, 255)  // Alice Blue
    )

    init {
        for (i in 0..50) {
            bubbles.add(Bubble())
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bubbles.forEach { it.setDimensions(w, h) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bubbles.forEach { bubble ->
            paint.color = bubble.color
            canvas.drawCircle(bubble.x, bubble.y, bubble.radius, paint)
            bubble.update()
        }
        invalidate()
    }

    inner class Bubble {
        var x = 0f
        var y = 0f
        var radius = 0f
        var speedY = 0f
        var color = 0

        init {
            reset()
        }

        fun setDimensions(width: Int, height: Int) {
            x = Random.nextFloat() * width
            y = Random.nextFloat() * height
        }

        fun reset() {
            radius = Random.nextFloat() * 100 + 20
            speedY = Random.nextFloat() * 2 + 1
            color = colors.random()
            x = Random.nextFloat() * (width.takeIf { it > 0 } ?: 1000)
            y = (height.takeIf { it > 0 } ?: 2000) + radius
        }

        fun update() {
            y -= speedY
            if (y < -radius) {
                reset()
            }
        }
    }
}
