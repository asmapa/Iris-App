package com.example.iris_new.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.iris_new.databinding.ActivityTeachFaceBinding
import com.example.iris_new.face.FaceEmbeddingExtractor
import com.example.iris_new.face.FaceRepository
import com.example.iris_new.face.db.FaceDatabase
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.example.iris_new.ui.adapter.FaceAdapter
import com.example.iris_new.face.db.FaceEntity

class TeachFaceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeachFaceBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    private lateinit var extractor: FaceEmbeddingExtractor
    private lateinit var repository: FaceRepository
    private lateinit var adapter: FaceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTeachFaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        extractor = FaceEmbeddingExtractor(this)
        repository = FaceRepository(
            FaceDatabase.getInstance(this).faceDao()
        )

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

            val selector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    selector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Camera error", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // ---------------- UI ----------------
    private fun loadFaces() {
        lifecycleScope.launch {
            val faces = repository.getAllFaces()
            adapter.updateData(faces)
        }
    }

    private fun setupUi() {

        // Capture + save face
        binding.captureButton.setOnClickListener {
            val name = binding.nameInput.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Enter a name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            captureAndSaveFace(name)
        }


        // Adapter will be connected later (faces list)
        adapter = FaceAdapter(emptyList()) { face ->

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete face")
                .setMessage("Remove ${face.name}?")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        repository.deleteFace(face.id)
                        loadFaces()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }


        binding.facesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.facesRecyclerView.adapter = adapter

    }

    // ---------------- FACE CAPTURE ----------------

    private fun captureAndSaveFace(name: String) {
        val capture = imageCapture ?: return

        val photoFile =
            File(externalCacheDir, "teach_face.jpg")

        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    output: ImageCapture.OutputFileResults
                ) {
                    val bitmap =
                        android.graphics.BitmapFactory
                            .decodeFile(photoFile.absolutePath)

                    lifecycleScope.launch {
                        val embedding =
                            extractor.extract(bitmap)

                        repository.addFace(name, embedding)

                        Toast.makeText(
                            this@TeachFaceActivity,
                            "Face saved for $name",
                            Toast.LENGTH_SHORT
                        ).show()

                        binding.nameInput.text.clear()
                        photoFile.delete()
                        loadFaces()
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(
                        this@TeachFaceActivity,
                        "Capture failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
