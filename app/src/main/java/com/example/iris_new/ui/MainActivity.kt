package com.example.iris_new.ui

import android.Manifest
import android.graphics.Color
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Vibrator
import org.tensorflow.lite.task.vision.detector.Detection
import com.example.iris_new.ObjectDetectionActivity
import android.speech.RecognitionListener
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import com.example.iris_new.core.system.PhoneStatusManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.iris_new.core.event.IrisEventBus
import com.example.iris_new.core.event.IrisEvent
import com.example.iris_new.databinding.ActivityMainBinding
import com.example.iris_new.ml.obstacle.ObjectDetectorAnalyzer
import com.example.iris_new.output.haptic.HapticManager
import com.example.iris_new.output.tts.TextToSpeechManager
import com.example.iris_new.voice.processCommand
import com.example.iris_new.camera.CameraManager
import com.example.iris_new.emergency.EmergencyManager
import com.example.iris_new.ml.ocr.OcrManager
import com.example.iris_new.ml.scene.SceneDescriptionManager
import com.example.iris_new.navigation.NavigationManager
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.Executors
import com.example.iris_new.core.color.ColorDetectionManager
import android.graphics.BitmapFactory
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.common.InputImage
import java.io.File
import android.util.Log
import com.example.iris_new.core.vision.ObjectDetectorHelper

import com.example.iris_new.face.FaceAnalyzer
import com.example.iris_new.face.FaceEmbeddingExtractor
import com.example.iris_new.face.FaceRecognitionManager
import com.example.iris_new.face.FaceRepository
import com.example.iris_new.face.db.FaceDatabase
import com.example.iris_new.face.RecognitionOverlayView


import com.google.mlkit.vision.text.latin.TextRecognizerOptions


class MainActivity : AppCompatActivity() {


    //craeted a variable for ActivityMainBinding which is automatically create dby android with activity_main.xml
    private lateinit var binding: ActivityMainBinding
    private lateinit var speechRecognizer: SpeechRecognizer


    private lateinit var speechIntent: Intent
    private lateinit var recognitionOverlay: RecognitionOverlayView
    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private val emergencyPermissions = arrayOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.SEND_SMS,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.all { it.value }) {
                startCamera()
                startSpeechRecognizer()
            }
        }

    private fun setupUiActions() {

        binding.readTextButton.setOnClickListener {
            lifecycleScope.launch {
                IrisEventBus.publish(IrisEvent.ReadText)
            }
        }

        binding.describeButton.setOnClickListener {
            lifecycleScope.launch {
                IrisEventBus.publish(IrisEvent.DescribeScene)
            }
        }

        binding.helpButton.setOnClickListener {
            lifecycleScope.launch {
                IrisEventBus.publish(IrisEvent.EmergencyTriggered)
            }
        }

        binding.objectDetectButton.setOnClickListener {
            lifecycleScope.launch {
                IrisEventBus.publish(
                    IrisEvent.Speak("Obstacle detection is running")
                )
            }
        }

        binding.settingsButton.setOnClickListener {
            startActivity(
                Intent(this, SettingsActivity::class.java)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        //here is where the main page is gonna open
        super.onCreate(savedInstanceState)
        //object is created for ActivityMainBinding, u can access any buttons
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recognitionOverlay = binding.recognitionOverlay
        lifecycleScope.launch {
            IrisEventBus.events.collect { event ->

                if (event is IrisEvent.FaceBox) {

                    recognitionOverlay.setRect(
                        event.rect,
                        event.imageWidth,
                        event.imageHeight,
                        false
                    )
                }
            }
        }

        // Initialize subscribers

        //objects get created here ,lifecyclescope means it run until the user close the screen
        TextToSpeechManager(this, lifecycleScope)
        HapticManager(
            getSystemService(Vibrator::class.java),
            lifecycleScope
        )




        //Event calling for currency detection
        lifecycleScope.launch {
            IrisEventBus.events.collect { event ->




                if (event is IrisEvent.StartFind) {

                    val intent = Intent(
                        this@MainActivity,
                        ObjectDetectionActivity::class.java
                    )

                    intent.putExtra("TARGET_OBJECT", event.target)

                    startActivity(intent)
                }


                if (event is IrisEvent.DetectCurrency) {
                    Log.d("CURRENCY_DEBUG", "DetectCurrency event received")

                    IrisEventBus.publish(
                        IrisEvent.Speak("Hold the note steady")
                    )

                    cameraManager.imageCapture?.let { capture ->
                        captureCurrencyImage(capture)
                    }
                }
            }
        }

        NavigationManager(
            context = this,
            scope = lifecycleScope
        )

        EmergencyManager(
            context = this,
            scope = lifecycleScope
        )



        PhoneStatusManager(
            context = this,
            scope = lifecycleScope
        )

        setupUiActions()

        if (allPermissionsGranted()) {
            startCamera()
            startSpeechRecognizer()
        } else {
            permissionLauncher.launch(REQUIRED_PERMISSIONS + emergencyPermissions)
        }

    }

    // ---------------- CAMERA ----------------
    private lateinit var cameraManager: CameraManager

    private fun startCamera() {

        val obstacleAnalyzer = ObjectDetectorAnalyzer(
            context = this,
            scope = lifecycleScope
        )
        val faceRepository =
            FaceRepository(
                FaceDatabase.getInstance(this).faceDao()
            )

        FaceRecognitionManager(
            repository = faceRepository,
            scope = lifecycleScope
        )



        // Face analyzer now ONLY detects faces
        val faceAnalyzer = FaceAnalyzer(
            context = this,
            scope = lifecycleScope,
            extractor = FaceEmbeddingExtractor(this)
        )

        val compositeAnalyzer = CompositeAnalyzer(
            listOf(
                obstacleAnalyzer,
                faceAnalyzer
            )
        )

        cameraManager = CameraManager(
            context = this,
            lifecycleOwner = this,
            executor = cameraExecutor,
            analyzer = compositeAnalyzer
        )

        cameraManager.startCamera(
            binding.viewFinder.surfaceProvider
        )

        // Scene description
        SceneDescriptionManager(
            context = this,
            imageCapture = cameraManager.imageCapture,
            scope = lifecycleScope
        )

        ColorDetectionManager(
            context = this,
            imageCapture = cameraManager.imageCapture,
            scope = lifecycleScope
        )

        // OCR
        OcrManager(
            context = this,
            imageCapture = cameraManager.imageCapture,
            scope = lifecycleScope
        )
    }


    private fun captureCurrencyImage(imageCapture: ImageCapture) {

        val file = File(filesDir, "currency.jpg")

        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),

            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    outputFileResults: ImageCapture.OutputFileResults
                ) {
                    Log.d("CURRENCY_DEBUG", "Image captured successfully")
                    processCurrencyImage(file)
                }

                override fun onError(exception: ImageCaptureException) {
                    lifecycleScope.launch {
                        IrisEventBus.publish(
                            IrisEvent.Speak("Failed to capture image")
                        )
                    }
                }
            }
        )
    }

    private fun processCurrencyImage(file: File) {

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)

        if (bitmap == null) {
            Log.e("CURRENCY_DEBUG", "Bitmap is NULL")
            return
        }

        Log.d("CURRENCY_DEBUG", "Bitmap loaded ${bitmap.width}x${bitmap.height}")

        val image = InputImage.fromBitmap(bitmap, 0)

        val recognizer =
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->

                val ocrText = visionText.text.lowercase()
                Log.d("CURRENCY_DEBUG", "OCR TEXT: $ocrText")

                val validNotes = listOf("10", "20", "50", "100", "200", "500","2000")

                val numbers = Regex("\\d+")
                    .findAll(ocrText)
                    .map { it.value }
                    .toList()

                Log.d("CURRENCY_DEBUG", "Numbers detected: $numbers")

                val detectedAmount =
                    numbers.firstOrNull { it in validNotes }

                lifecycleScope.launch {

                    if (detectedAmount != null) {
                        Log.d("CURRENCY_DEBUG", "Detected: $detectedAmount")
                        IrisEventBus.publish(
                            IrisEvent.Speak("$detectedAmount rupees")
                        )
                    } else {
                        Log.d("CURRENCY_DEBUG", "No valid currency found")
                        IrisEventBus.publish(
                            IrisEvent.Speak("Unable to identify the amount")
                        )
                    }
                }
            }
            .addOnFailureListener {
                Log.e("CURRENCY_DEBUG", "OCR failed", it)
                lifecycleScope.launch {
                    IrisEventBus.publish(
                        IrisEvent.Speak("Text recognition failed")
                    )
                }
            }

    }

    // ---------------- SPEECH ----------------

    private fun startSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text =
                    results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )?.firstOrNull() ?: return

                lifecycleScope.launch {
                    processCommand(text.lowercase())
                }
            }


            override fun onEndOfSpeech() = startListening()
            override fun onError(error: Int) = startListening()

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        startListening()

    }

    private fun startListening() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer.startListening(speechIntent)
        }
    }

    // ---------------- PERMISSIONS ----------------

    private fun allPermissionsGranted() =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                this, it
            ) == PackageManager.PERMISSION_GRANTED
        }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        speechRecognizer.destroy()
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}
