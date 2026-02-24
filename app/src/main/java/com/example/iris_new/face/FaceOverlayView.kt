package com.example.iris_new.face

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // ONE rectangle only
    private var faceRect: Rect? = null

    private val paint = Paint().apply {
        color = 0xFF00FF00.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    // Called from analyzer
    fun setRect(rect: Rect, imageWidth: Int, imageHeight: Int, isFrontCamera: Boolean) {

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        if (viewWidth == 0f || viewHeight == 0f) return

        // Scale factors
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight

        // Scale rect from camera -> overlay view
        var scaledRect = Rect(
            (rect.left * scaleX).toInt(),
            (rect.top * scaleY).toInt(),
            (rect.right * scaleX).toInt(),
            (rect.bottom * scaleY).toInt()
        )

        // Mirror for front camera
        if (isFrontCamera) {
            scaledRect = Rect(
                viewWidth.toInt() - scaledRect.right,
                scaledRect.top,
                viewWidth.toInt() - scaledRect.left,
                scaledRect.bottom
            )
        }

        faceRect = scaledRect
        invalidate()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        val ovalWidth = width * 0.38f
        val ovalHeight = height * 0.75f


        val left = centerX - ovalWidth / 2
        val top = centerY - ovalHeight / 2
        val right = centerX + ovalWidth / 2
        val bottom = centerY + ovalHeight / 2

        val rectF = RectF(left, top, right, bottom)

        canvas.drawOval(rectF, paint)
    }

}
