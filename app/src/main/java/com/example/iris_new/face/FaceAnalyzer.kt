package com.example.iris_new.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.iris_new.core.event.IrisEvent
import com.example.iris_new.core.event.IrisEventBus
import com.example.iris_new.core.state.AttentionController
import com.example.iris_new.core.state.AttentionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch




class FaceAnalyzer(
    private val context: Context,
    private val scope: CoroutineScope,
    private val extractor: FaceEmbeddingExtractor
) : ImageAnalysis.Analyzer {

    companion object {
        var teachingMode = false
    }

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )

    private var lastProcessedTime = 0L
    private val PROCESS_INTERVAL_MS = 800L

    override fun analyze(imageProxy: ImageProxy) {

        try {

            Log.d("FACE_ANALYZER", "Frame received")

            // 🔒 Attention state check
            if (AttentionController.state.value == AttentionState.BUSY) {
                Log.d("FACE_ANALYZER", "Skipped - BUSY state")
                return
            }

            val now = System.currentTimeMillis()

            // ⏱️ Frame throttle
            if (now - lastProcessedTime < PROCESS_INTERVAL_MS) {
                Log.d("FACE_ANALYZER", "Skipped - too fast")
                return
            }

            lastProcessedTime = now

            // 🔄 Convert to bitmap
            val bitmap = try {
                imageProxy.toBitmap().copy(Bitmap.Config.ARGB_8888, false)
            } catch (e: Exception) {
                Log.e("FACE_ANALYZER", "Bitmap conversion failed", e)
                return
            }

            // 🔁 Create ML input
            val inputImage = try {
                InputImage.fromBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)
            } catch (e: Exception) {
                Log.e("FACE_ANALYZER", "InputImage creation failed", e)
                return
            }

            // 🧠 Detect face
            detector.process(inputImage)
                .addOnSuccessListener { faces ->

                    // 🔒 stop if system became busy while ML running
                    if (AttentionController.state.value == AttentionState.BUSY) {
                        Log.d("FACE_ANALYZER", "Cancelled after detection - system busy")
                        return@addOnSuccessListener
                    }

                    try {

                        if (faces.isEmpty()) {
                            Log.d("FACE_ANALYZER", "No face found")
                            return@addOnSuccessListener
                        }

                        Log.d("FACE_ANALYZER", "FACE FOUND")

                        val face = faces[0]
                        val box = face.boundingBox

                        // 🟢 Oval check
                        val faceCenterX = box.centerX()
                        val faceCenterY = box.centerY()

                        val ovalCenterX = bitmap.width / 2
                        val ovalCenterY = bitmap.height / 2

                        val allowedWidth = bitmap.width * 0.25
                        val allowedHeight = bitmap.height * 0.30

                        val isInsideOval =
                            faceCenterX in (ovalCenterX - allowedWidth).toInt()..(ovalCenterX + allowedWidth).toInt() &&
                                    faceCenterY in (ovalCenterY - allowedHeight).toInt()..(ovalCenterY + allowedHeight).toInt()

                        if (!isInsideOval) {
                            Log.d("FACE_ANALYZER", "Face outside oval")

                            scope.launch {
                                try {
                                    IrisEventBus.publish(
                                        IrisEvent.Speak("Adjust face inside the oval")
                                    )
                                } catch (e: Exception) {
                                    Log.e("FACE_ANALYZER", "Speak publish failed", e)
                                }
                            }
                            return@addOnSuccessListener
                        }

                        Log.d("FACE_ANALYZER", "Face inside oval")

                        // 📦 Send box to overlay
                        scope.launch {
                            try {
                                IrisEventBus.publish(
                                    IrisEvent.FaceBox(
                                        rect = box,
                                        imageWidth = bitmap.width,
                                        imageHeight = bitmap.height
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e("FACE_ANALYZER", "FaceBox publish failed", e)
                            }
                        }

                        // ✂️ Crop face
                        val croppedFace = try {

                            val centerX = box.centerX()
                            val centerY = box.centerY()

                            val cropWidth = (box.width() * 0.75f).toInt()
                            val cropHeight = (box.height() * 0.90f).toInt()

                            val left = (centerX - cropWidth / 2).coerceAtLeast(0)
                            val top = (centerY - cropHeight / 2).coerceAtLeast(0)
                            val right = (centerX + cropWidth / 2).coerceAtMost(bitmap.width)
                            val bottom = (centerY + cropHeight / 2).coerceAtMost(bitmap.height)

                            Bitmap.createBitmap(
                                bitmap,
                                left,
                                top,
                                right - left,
                                bottom - top
                            )

                        } catch (e: Exception) {
                            Log.e("FACE_ANALYZER", "Face crop failed", e)
                            return@addOnSuccessListener
                        }

                        // 🧬 Extract embedding
                        val embedding = try {
                            extractor.extract(croppedFace)
                        } catch (e: Exception) {
                            Log.e("FACE_ANALYZER", "Embedding extraction failed", e)
                            return@addOnSuccessListener
                        }

                        Log.d("FACE_ANALYZER", "Embedding extracted")

                        // 📡 Publish result
                        scope.launch {
                            try {

                                Log.d("EMBEDDING_FLOW", "Embedding extracted")
                                if (AttentionController.state.value != AttentionState.FREE

                                    ) return@launch

                                if (teachingMode) {
                                    Log.d("EMBEDDING_FLOW", "Teaching mode active")
                                    scope.launch {
                                        IrisEventBus.publish(IrisEvent.TeachingEmbedding(embedding))
                                    }
                                } else {
                                    Log.d("EMBEDDING_FLOW", "Recognition mode active")
                                    scope.launch {
                                        IrisEventBus.publish(IrisEvent.FaceDetected(embedding))
                                    }
                                }


                            } catch (e: Exception) {
                                Log.e("FACE_ANALYZER", "Event publish failed", e)
                            }
                        }

                    } catch (e: Exception) {
                        Log.e("FACE_ANALYZER", "Face pipeline crash", e)
                    }
                }

                .addOnFailureListener {
                    Log.e("FACE_ANALYZER", "Face detection failed", it)
                }

        } catch (e: Exception) {
            Log.e("FACE_ANALYZER", "Analyzer crash", e)
        } finally {
            // 🚨 MUST — or camera dies
            imageProxy.close()
        }
    }

}
