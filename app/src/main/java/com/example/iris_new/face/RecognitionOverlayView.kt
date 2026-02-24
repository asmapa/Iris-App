package com.example.iris_new.face

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class RecognitionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var faceRect: Rect? = null

    private val paint = Paint().apply {
        color = 0xFF00FF00.toInt()
        style = Paint.Style.STROKE //no fillings needed in that oval
        strokeWidth = 8f
    }

    fun setRect(rect: Rect, imageWidth: Int, imageHeight: Int, isFrontCamera: Boolean) {

        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        // 🔥 SHRINK THE FACE BOX
        val shrinkWidthFactor = 0.65f   // reduce side area
        val shrinkHeightFactor = 0.70f  // reduce top/bottom (remove neck & hair)

        val centerX = rect.centerX()
        val centerY = rect.centerY()

        val newWidth = rect.width() * shrinkWidthFactor
        val newHeight = rect.height() * shrinkHeightFactor

        val left = (centerX - newWidth / 2).toInt()
        val top = (centerY - newHeight / 2).toInt()
        val right = (centerX + newWidth / 2).toInt()
        val bottom = (centerY + newHeight / 2).toInt()

        val tightRect = Rect(left, top, right, bottom)

        // scale to screen
        var scaledRect = Rect(
            (tightRect.left * scaleX).toInt(),
            (tightRect.top * scaleY).toInt(),
            (tightRect.right * scaleX).toInt(),
            (tightRect.bottom * scaleY).toInt()
        )

        // mirror if front cam
        if (isFrontCamera) {
            scaledRect = Rect(
                width - scaledRect.right,
                scaledRect.top,
                width - scaledRect.left,
                scaledRect.bottom
            )
        }

        faceRect = scaledRect
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        faceRect?.let { rect ->
            val rectF = RectF(rect)

            val paddingX = rectF.width() * 0.25f
            val paddingY = rectF.height() * 0.40f

            rectF.left -= paddingX
            rectF.right += paddingX
            rectF.top -= paddingY
            rectF.bottom += paddingY

            canvas.drawOval(rectF, paint)
        }
    }
}