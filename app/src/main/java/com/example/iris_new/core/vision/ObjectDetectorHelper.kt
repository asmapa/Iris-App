package com.example.iris_new.core.vision

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log

import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class ObjectDetectorHelper(

    private val context: Context,
    private val detectorListener: DetectorListener?

) {

    private var objectDetector: ObjectDetector? = null

    private val threshold = 0.5f
    private val maxResults = 5
    private val numThreads = 2

    init {
        setupObjectDetector()
    }

    private fun setupObjectDetector() {

        val optionsBuilder =
            ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(threshold)
                .setMaxResults(maxResults)

        val baseOptionsBuilder = BaseOptions.builder()
            .setNumThreads(numThreads)


        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {

            objectDetector =
                ObjectDetector.createFromFileAndOptions(
                    context,
                    "efficientdet.tflite",
                    optionsBuilder.build()
                )

        } catch (e: Exception) {

            detectorListener?.onError(
                "Failed to load EfficientDet model"
            )

            Log.e("IRIS", "Model loading error: ${e.message}")
        }
    }

    fun detect(bitmap: Bitmap, rotation: Int) {

        if (objectDetector == null) {
            setupObjectDetector()
        }

        var inferenceTime = SystemClock.uptimeMillis()

        val imageProcessor =
            ImageProcessor.Builder()
                .add(Rot90Op(-rotation / 90))
                .build()

        val tensorImage =
            imageProcessor.process(TensorImage.fromBitmap(bitmap))

        val results = objectDetector?.detect(tensorImage)

        inferenceTime =
            SystemClock.uptimeMillis() - inferenceTime

        detectorListener?.onResults(
            results,
            inferenceTime,
            tensorImage.height,
            tensorImage.width
        )
    }

    interface DetectorListener {

        fun onError(error: String)

        fun onResults(
            results: MutableList<Detection>?,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }
}