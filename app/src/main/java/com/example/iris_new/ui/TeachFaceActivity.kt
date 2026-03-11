
package com.example.iris_new.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.example.iris_new.ui.adapter.FaceAdapter
import com.example.iris_new.core.event.IrisEventBus
import com.example.iris_new.core.event.IrisEvent

class TeachFaceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeachFaceBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    private lateinit var extractor: FaceEmbeddingExtractor
    private lateinit var repository: FaceRepository
    private lateinit var adapter: FaceAdapter

    private lateinit var faceOverlay: FaceOverlayView

    private var waitingForCapture = false

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

                    lifecycleScope.launch {
                        repository.addFace(name, event.embedding)
                        loadFaces()
                    }

                    val msg = "Face captured for $name"

                    Toast.makeText(
                        this@TeachFaceActivity,
                        msg,
                        Toast.LENGTH_SHORT
                    ).show()

                    lifecycleScope.launch {
                        IrisEventBus.publish(IrisEvent.Speak(msg))
                    }

                    waitingForCapture = false
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
                imageAnalysis
            )

        }, ContextCompat.getMainExecutor(this))
    }

    // ---------------- UI ----------------

    private fun loadFaces() {
        lifecycleScope.launch {

            val faces = repository.getAllFaces()

            // show only one entry per person in UI
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
            FaceAnalyzer.teachingMode = true

            val msg = "Look at the camera"

            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

            lifecycleScope.launch {
                IrisEventBus.publish(IrisEvent.Speak(msg))
            }
        }

        adapter = FaceAdapter(emptyList()) { face ->

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete face")
                .setMessage("Remove ${face.name}?")
                .setPositiveButton("Delete") { _, _ ->

                    lifecycleScope.launch {

                        repository.deleteFaceByName(face.name)
                        loadFaces()

                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.facesRecyclerView.layoutManager =
            LinearLayoutManager(this)

        binding.facesRecyclerView.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()

        FaceAnalyzer.teachingMode = false
        cameraExecutor.shutdown()
    }
}

