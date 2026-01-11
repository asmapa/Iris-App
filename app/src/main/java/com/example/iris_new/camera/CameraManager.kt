package com.example.iris_new.camera


import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.iris_new.ml.obstacle.ObjectDetectorAnalyzer
import com.example.iris_new.ml.scene.SceneDescriptionManager
import com.example.iris_new.ui.CompositeAnalyzer
import java.util.concurrent.ExecutorService

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val executor: ExecutorService,
    private val analyzer: CompositeAnalyzer
) {

    // 🔹 ImageCapture is OWNED by CameraManager
    val imageCapture: ImageCapture = ImageCapture.Builder().build()

    fun startCamera(surfaceProvider: Preview.SurfaceProvider) {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(
                    ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                )
                .build()
                .also {
                    it.setAnalyzer(executor, analyzer)
                }

            cameraProvider.unbindAll()

            // ✅ imageCapture is bound HERE
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis,
                imageCapture
            )

        }, ContextCompat.getMainExecutor(context))
    }
}
