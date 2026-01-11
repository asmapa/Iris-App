package com.example.iris_new.face

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
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

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )

    private var lastProcessedTime = 0L
    private val PROCESS_INTERVAL_MS = 800L // 🔥 adjust 500–1000ms

    override fun analyze(imageProxy: ImageProxy) {

        // 🔒 Respect global attention state
        if (AttentionController.state.value == AttentionState.BUSY) return

        val now = System.currentTimeMillis()

        // ⏱️ FRAME SKIP
        if (now - lastProcessedTime < PROCESS_INTERVAL_MS) {
            return
        }

        lastProcessedTime = now

        try {
            // ✅ SAFE: convert immediately
            val bitmap = imageProxy
                .toBitmap()
                .copy(Bitmap.Config.ARGB_8888, false)
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        try {
                            val embedding = extractor.extract(bitmap)

                            scope.launch {
                                IrisEventBus.publish(
                                    IrisEvent.FaceDetected(embedding)
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("FaceAnalyzer", "Embedding failed", e)
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("FaceAnalyzer", "Face detection failed", it)
                }

        } catch (e: Exception) {
            Log.e("FaceAnalyzer", "Analyzer error", e)
        }
    }
}
