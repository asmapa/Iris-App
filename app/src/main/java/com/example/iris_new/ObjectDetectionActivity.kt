package com.example.iris_new

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.graphics.SurfaceTexture
import android.view.TextureView
import android.widget.ImageView
import androidx.core.content.ContextCompat
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.iris_new.core.event.IrisEvent
import com.example.iris_new.core.event.IrisEventBus
import com.example.iris_new.R

class ObjectDetectionActivity : AppCompatActivity() {

    lateinit var textureView: TextureView
    lateinit var imageView: ImageView

    lateinit var cameraDevice: CameraDevice
    lateinit var handler: Handler
    lateinit var cameraManager: CameraManager

    lateinit var bitmap: Bitmap
    lateinit var detector: ObjectDetector

    val paint = Paint()
    private var targetObject: String? = null
    private var objectFound = false

    val colors = listOf(
        Color.BLUE,
        Color.GREEN,
        Color.RED,
        Color.CYAN,
        Color.MAGENTA,
        Color.YELLOW
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_object_detection)

        getPermission()

        textureView = findViewById(R.id.textureView)
        imageView = findViewById(R.id.imageView)

        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setScoreThreshold(0.5f)
            .setMaxResults(5)
            .build()

        detector = ObjectDetector.createFromFileAndOptions(
            this,
            "efficientdet.tflite",
            options
        )

        lifecycleScope.launch {

            IrisEventBus.events.collect { event ->

                if (event is IrisEvent.GoBack) {

                    runOnUiThread {
                        finish()
                    }

                }

            }
        }


        targetObject = intent.getStringExtra("TARGET_OBJECT")?.lowercase()

        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        textureView.surfaceTextureListener =
            object : TextureView.SurfaceTextureListener {

                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {
                    openCamera()
                }

                override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {}

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    return false
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

                    bitmap = textureView.bitmap ?: return

                    val image = TensorImage.fromBitmap(bitmap)

                    val results = detector.detect(image)

                    val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    val canvas = Canvas(mutable)

                    val h = mutable.height
                    val w = mutable.width

                    paint.textSize = h / 15f
                    paint.strokeWidth = h / 85f

                    results.forEachIndexed { index, detection ->

                        val category = detection.categories.firstOrNull()
                            ?: return@forEachIndexed

                        val label = category.label.lowercase()
                        val score = category.score

                        val box = detection.boundingBox

                        // -------- DRAW RECTANGLE FOR ALL OBJECTS --------
                        paint.color = colors[index % colors.size]
                        paint.style = Paint.Style.STROKE

                        canvas.drawRect(
                            RectF(
                                box.left,
                                box.top,
                                box.right,
                                box.bottom
                            ),
                            paint
                        )

                        paint.style = Paint.Style.FILL

                        canvas.drawText(
                            "$label ${"%.2f".format(score)}",
                            box.left,
                            box.top,
                            paint
                        )

                        // -------- CHECK TARGET OBJECT --------
                        if (!objectFound && targetObject != null && label.contains(targetObject!!)) {

                            objectFound = true

                            val centerX = (box.left + box.right) / 2
                            val screenWidth = bitmap.width

                            val position = when {
                                centerX < screenWidth * 0.33 -> "left"
                                centerX < screenWidth * 0.66 -> "center"
                                else -> "right"
                            }

                            lifecycleScope.launch {
                                IrisEventBus.publish(
                                    IrisEvent.Speak("$label detected on the $position")
                                )
                            }

                            runOnUiThread {
                                finish()
                            }

                            return@forEachIndexed
                        }
                    }

                    imageView.setImageBitmap(mutable)
                }
            }

        cameraManager =
            getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    fun openCamera() {

        cameraManager.openCamera(
            cameraManager.cameraIdList[0],
            object : CameraDevice.StateCallback() {

                override fun onOpened(camera: CameraDevice) {

                    cameraDevice = camera

                    val surfaceTexture = textureView.surfaceTexture
                    val surface = Surface(surfaceTexture)

                    val captureRequest =
                        cameraDevice.createCaptureRequest(
                            CameraDevice.TEMPLATE_PREVIEW
                        )

                    captureRequest.addTarget(surface)

                    cameraDevice.createCaptureSession(
                        listOf(surface),
                        object : CameraCaptureSession.StateCallback() {

                            override fun onConfigured(session: CameraCaptureSession) {

                                session.setRepeatingRequest(
                                    captureRequest.build(),
                                    null,
                                    handler
                                )
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {}

                        },
                        handler
                    )
                }

                override fun onDisconnected(camera: CameraDevice) {}

                override fun onError(camera: CameraDevice, error: Int) {}
            },
            handler
        )
    }

    fun getPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(android.Manifest.permission.CAMERA),
                101
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            getPermission()
        }
    }
}