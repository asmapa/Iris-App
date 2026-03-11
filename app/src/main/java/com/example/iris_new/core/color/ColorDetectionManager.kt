package com.example.iris_new.core.color

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import com.example.iris_new.core.event.IrisEvent
import com.example.iris_new.core.event.IrisEventBus
import com.example.iris_new.core.state.AttentionController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

class ColorDetectionManager(
    private val context: Context,
    private val imageCapture: ImageCapture?,
    private val scope: CoroutineScope
) {

    init {

        Log.d("COLOR_DEBUG", "ColorDetectionManager initialized")

        scope.launch {

            IrisEventBus.events.collect { event ->

                Log.d("COLOR_DEBUG", "Event received: $event")

                if (event is IrisEvent.DetectColor) {

                    Log.d("COLOR_DEBUG", "DetectColor event triggered")

                    // 🔒 Lock attention
                    AttentionController.lock()
                    Log.d("COLOR_DEBUG", "Attention locked")

                    IrisEventBus.publish(
                        IrisEvent.Speak("Checking the color")
                    )

                    captureImage()
                }
            }
        }
    }

    private fun captureImage() {

        Log.d("COLOR_DEBUG", "captureImage() called")

        val capture = imageCapture

        if (capture == null) {

            Log.e("COLOR_DEBUG", "ImageCapture is NULL")

            scope.launch {
                IrisEventBus.publish(
                    IrisEvent.Speak("Camera not ready")
                )
            }

            AttentionController.release()
            return
        }

        val file = File(context.filesDir, "color.jpg")

        Log.d("COLOR_DEBUG", "Saving image to ${file.absolutePath}")

        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(file).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),

            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    outputFileResults: ImageCapture.OutputFileResults
                ) {

                    Log.d("COLOR_DEBUG", "Image captured successfully")

                    processImage(file)
                }

                override fun onError(exception: ImageCaptureException) {

                    Log.e("COLOR_DEBUG", "Image capture failed", exception)

                    scope.launch {
                        IrisEventBus.publish(
                            IrisEvent.Speak("Failed to capture image")
                        )
                    }

                    AttentionController.release()
                }
            }
        )
    }

    private fun processImage(file: File) {

        Log.d("COLOR_DEBUG", "Processing image")

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)

        if (bitmap == null) {

            Log.e("COLOR_DEBUG", "Bitmap is NULL")

            AttentionController.release()
            return
        }

        Log.d(
            "COLOR_DEBUG",
            "Bitmap loaded ${bitmap.width}x${bitmap.height}"
        )

        val color = ColorAnalyzer.detectDominantColor(bitmap)

        Log.d("COLOR_DEBUG", "Detected color: $color")

        scope.launch {

            IrisEventBus.publish(
                IrisEvent.Speak("The color is $color")
            )

            Log.d("COLOR_DEBUG", "Color spoken")

            // 🔓 Release attention
            AttentionController.release()

            Log.d("COLOR_DEBUG", "Attention released")
        }
    }
}