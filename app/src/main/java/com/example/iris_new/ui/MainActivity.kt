package com.example.iris_new.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
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
import com.example.iris_new.face.FaceAnalyzer
import com.example.iris_new.face.FaceEmbeddingExtractor
import com.example.iris_new.face.FaceRecognitionManager
import com.example.iris_new.face.FaceRepository
import com.example.iris_new.face.db.FaceDatabase


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechIntent: Intent

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
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Initialize subscribers
        TextToSpeechManager(this, lifecycleScope)
        HapticManager(
            getSystemService(Vibrator::class.java),
            lifecycleScope
        )

        NavigationManager(
            context = this,
            scope = lifecycleScope
        )

        EmergencyManager(
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
            FaceRepository(FaceDatabase.getInstance(this).faceDao())

        FaceRecognitionManager(
            repository = faceRepository,
            scope = lifecycleScope
        )

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

        // 🔹 Initialize SceneDescriptionManager HERE
        SceneDescriptionManager(
            context = this,
            imageCapture = cameraManager.imageCapture,
            scope = lifecycleScope
        )

        OcrManager(
            context = this,
            imageCapture = cameraManager.imageCapture,
            scope = lifecycleScope
        )

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
