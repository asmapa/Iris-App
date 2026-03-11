package com.example.iris_new.ml.ocr

import android.content.Context
import android.graphics.BitmapFactory
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.example.iris_new.core.event.IrisEvent
import com.example.iris_new.core.event.IrisEventBus
import com.example.iris_new.core.state.AttentionController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

class OcrManager(
    private val context: Context,
    private val imageCapture: ImageCapture,
    private val scope: CoroutineScope
) {

    private var stopRequested = false
    private val recognizer =
        TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS
        )

    init {
        scope.launch {
            IrisEventBus.events.collect { event ->

                if (event is IrisEvent.ReadText) {
                    stopRequested = false
                    readText()
                }

                if (event is IrisEvent.StopReading) {
                    stopRequested = true
                }

            }
        }
    }

    private fun readText() {

        if (stopRequested) return
        AttentionController.lock()

        val photoDir = context.getExternalFilesDir(null)
        val photoFile = File(photoDir, "ocr.jpg")

        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    output: ImageCapture.OutputFileResults
                ) {
                    scope.launch {
                        try {
                            IrisEventBus.publish(
                                IrisEvent.Speak("Reading text")
                            )

                            val bitmap =
                                BitmapFactory.decodeFile(photoFile.absolutePath)

                            val image =
                                InputImage.fromBitmap(bitmap, 0)

                            recognizer.process(image)
                                .addOnSuccessListener { result ->

                                    if (stopRequested) return@addOnSuccessListener
                                    val text = result.text

                                    if (text.isNotBlank()) {
                                        scope.launch {
                                            IrisEventBus.publish(
                                                IrisEvent.Speak(text)
                                            )
                                        }
                                    } else {
                                        scope.launch {
                                            IrisEventBus.publish(
                                                IrisEvent.Speak(
                                                    "No readable text found"
                                                )
                                            )
                                        }
                                    }

                                    photoFile.delete()
                                }
                                .addOnFailureListener {
                                    scope.launch {
                                        IrisEventBus.publish(
                                            IrisEvent.Speak(
                                                "Failed to read text"
                                            )
                                        )
                                    }
                                    photoFile.delete()
                                }

                        } catch (e: Exception) {
                            scope.launch {
                                IrisEventBus.publish(
                                    IrisEvent.Speak(
                                        "OCR error occurred"
                                    )
                                )
                            }
                            photoFile.delete()
                        }
                    }
                }

                override fun onError(
                    exception: ImageCaptureException
                ) {
                    scope.launch {
                        IrisEventBus.publish(
                            IrisEvent.Speak("Camera error")
                        )
                    }
                }
            }
        )
        AttentionController.release()
    }
}