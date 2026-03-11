package com.example.iris_new.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.iris_new.databinding.ActivityTeachFaceBinding
import com.example.iris_new.face.FaceEmbeddingExtractor
import com.example.iris_new.face.FaceOverlayView
import com.example.iris_new.face.FaceAnalyzer
import com.example.iris_new.face.FaceRepository
import com.example.iris_new.face.db.FaceDatabase
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.example.iris_new.ui.adapter.FaceAdapter
import com.example.iris_new.face.db.FaceEntity
import com.example.iris_new.core.event.IrisEventBus
import com.example.iris_new.core.event.IrisEvent
import android.util.Log


class TeachFaceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeachFaceBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    private lateinit var extractor: FaceEmbeddingExtractor
    private lateinit var repository: FaceRepository
    private lateinit var adapter: FaceAdapter


    private lateinit var faceOverlay: FaceOverlayView

    private var waitingForCapture = false
    private var captureState = 0 // 0=Center, 1=Left, 2=Right, 3=Up, 4=Down
    private val capturedEmbeddings = mutableListOf<FloatArray>()
    private var lastGuidanceTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTeachFaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        extractor = FaceEmbeddingExtractor(this)
        repository = FaceRepository(
            FaceDatabase.getInstance(this).faceDao()
        )


        faceOverlay = binding.faceOverlay
        faceOverlay.bringToFront()



        lifecycleScope.launch {
            IrisEventBus.events.collect { event ->
                if (event is IrisEvent.TeachingEmbedding && waitingForCapture) {
                    val name = binding.nameInput.text.toString().trim()
                    if (name.isEmpty()) return@collect

                    val yaw = event.yaw
                    val pitch = event.pitch
                    var stateMatched = false

                    when (captureState) {
                        0 -> if (yaw in -10f..10f && pitch in -10f..10f) stateMatched = true // Center
                        1 -> if (yaw < -15f) stateMatched = true // Left
                        2 -> if (yaw > 15f) stateMatched = true // Right
                        3 -> if (pitch > 10f) stateMatched = true // Up
                        4 -> if (pitch < -10f) stateMatched = true // Down
                    }

                    val now = System.currentTimeMillis()

                    if (stateMatched) {
                        capturedEmbeddings.add(event.embedding)
                        captureState++

                        if (captureState > 4) {
                            // Save all embeddings
                            for (emb in capturedEmbeddings) {
                                repository.addFace(name, emb)
                            }

                            val toastMsg = "Registration complete for $name!"
                            Toast.makeText(this@TeachFaceActivity, toastMsg, Toast.LENGTH_SHORT).show()
                            launch { IrisEventBus.publish(IrisEvent.Speak(toastMsg)) }

                            binding.nameInput.text.clear()
                            FaceAnalyzer.teachingMode = false
                            waitingForCapture = false
                        } else {
                            val guidance = getGuidanceMessage(captureState)
                            Toast.makeText(this@TeachFaceActivity, guidance, Toast.LENGTH_SHORT).show()
                            launch { IrisEventBus.publish(IrisEvent.Speak(guidance)) }
                            lastGuidanceTime = now
                        }
                    } else {
                        // Respeak guidance every 3 seconds if not matching
                        if (now - lastGuidanceTime > 3000) {
                            val guidance = getGuidanceMessage(captureState)
                            launch { IrisEventBus.publish(IrisEvent.Speak(guidance)) }
                            lastGuidanceTime = now
                        }
                    }
                }
            }
        }



        startCamera()
        setupUi()
        loadFaces()
    }

    // ---------------- CAMERA ----------------

    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.facePreview.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            // 🔥 ADD THIS BLOCK
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(
                cameraExecutor,
                FaceAnalyzer(this, lifecycleScope, extractor)
            )

            val selector = CameraSelector.DEFAULT_FRONT_CAMERA

            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(
                this,
                selector,
                preview,
                imageCapture,
                imageAnalysis // 🔥 THIS WAS MISSING
            )

        }, ContextCompat.getMainExecutor(this))
    }


    // ---------------- UI ----------------
    private fun loadFaces() {
        lifecycleScope.launch {
            val faces = repository.getAllFaces()
            // Group the 5 embeddings by name so the user only sees one entry per person
            val uniqueFaces = faces.distinctBy { it.name }
            adapter.updateData(uniqueFaces)
        }
    }

    private fun setupUi() {

        binding.captureButton.setOnClickListener {

            val name = binding.nameInput.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Enter a name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            waitingForCapture = true
            captureState = 0
            capturedEmbeddings.clear()
            FaceAnalyzer.teachingMode = true

            val startMessage = "Place face inside oval and look straight ahead"
            Toast.makeText(this, startMessage, Toast.LENGTH_SHORT).show()
            lifecycleScope.launch { IrisEventBus.publish(IrisEvent.Speak(startMessage)) }
            lastGuidanceTime = System.currentTimeMillis()
        }



        // Adapter will be connected later (faces list)
        adapter = FaceAdapter(emptyList()) { face ->

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete face")
                .setMessage("Remove ${face.name}?")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        // Delete ALL 5 embeddings associated with this person's name
                        repository.deleteFaceByName(face.name)
                        loadFaces()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }


        binding.facesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.facesRecyclerView.adapter = adapter

    }


    private fun getGuidanceMessage(state: Int): String {
        return when (state) {
            0 -> "Look straight ahead"
            1 -> "Turn your head slightly left"
            2 -> "Turn your head slightly right"
            3 -> "Tilt your head slightly up"
            4 -> "Tilt your head slightly down"
            else -> ""
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FaceAnalyzer.teachingMode = false

        cameraExecutor.shutdown()
    }
}
