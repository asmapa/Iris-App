package com.example.iris_new.ml.obstacle

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.iris_new.core.event.IrisEvent
import com.example.iris_new.core.event.IrisEventBus
import com.example.iris_new.core.state.AttentionController
import com.example.iris_new.core.state.AttentionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class ObjectDetectorAnalyzer(
    context: Context,
    private val scope: CoroutineScope
) : ImageAnalysis.Analyzer {

    private var detector: ObjectDetector? = null

    init {

        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(5)
            .setScoreThreshold(0.5f)
            .build()

        detector = ObjectDetector.createFromFileAndOptions(
            context,
            "efficientdet.tflite",
            options
        )
    }

    override fun analyze(imageProxy: ImageProxy) {

        // 🔒 Respect attention state
        if (AttentionController.state.value == AttentionState.BUSY) {
            imageProxy.close()
            return
        }

        try {

            val bitmap = imageProxy.toBitmap()
            val image = TensorImage.fromBitmap(bitmap)

            val results = detector?.detect(image).orEmpty()

            var maxSize = 0f

            for (detection in results) {

                val category = detection.categories.firstOrNull()

                val label: String = category?.label ?: "Unknown"
                val score: Float = category?.score ?: 0f

                Log.d(
                    "OBJECT_DETECTION",
                    "Detected: $label confidence=${"%.2f".format(score)}"
                )

                // 🔎 Publish detected object for FindController
                if (score > 0.6f) {

                    scope.launch {

                        IrisEventBus.publish(
                            IrisEvent.ObjectDetected(
                                label,
                                score
                            )
                        )
                    }
                }

                val heightRatio =
                    detection.boundingBox.height() / image.height.toFloat()

                maxSize = maxOf(maxSize, heightRatio)
            }

            // 🚧 Obstacle detection
            if (maxSize > 0.25f) {

                scope.launch {

                    IrisEventBus.publish(
                        IrisEvent.ObstacleDetected(maxSize)
                    )
                }
            }

        } catch (e: Exception) {

            Log.e(
                "ObstacleAnalyzer",
                "Obstacle detection failed",
                e
            )

        }
    }
}