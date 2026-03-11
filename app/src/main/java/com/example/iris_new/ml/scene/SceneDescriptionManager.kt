package com.example.iris_new.ml.scene


import android.content.Context
import android.graphics.BitmapFactory
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.example.iris_new.core.event.IrisEvent
import com.example.iris_new.core.event.IrisEventBus
import com.example.iris_new.core.state.AttentionController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class SceneDescriptionManager(
    private val context: Context,
    private val imageCapture: ImageCapture,
    private val scope: CoroutineScope
) {

    private val gemini = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = "AIzaSyCIZA9odIFaeFrA_-pZQH4dPsK5L5PEWts"
    )

    init {
        scope.launch {
            IrisEventBus.events.collect { event ->
                if (event is IrisEvent.DescribeScene) {
                    describeScene()
                }
            }
        }
    }

    private fun describeScene() {
        AttentionController.lock()


        val photoFile = File(context.externalMediaDirs.first(), "scene.jpg")

        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    output: ImageCapture.OutputFileResults
                ) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            IrisEventBus.publish(
                                IrisEvent.Speak("Analyzing scene")
                            )


                            val processingJob = launch {
                                while (true) {
                                    IrisEventBus.publish(
                                        IrisEvent.Speak("Processing")
                                    )
                                    kotlinx.coroutines.delay(2500) // speak every 2.5 sec
                                }
                            }

                            val bitmap =
                                BitmapFactory.decodeFile(photoFile.absolutePath)

                            val input = content {
                                image(bitmap)
                                text(
                                    "Describe this scene in one short sentence for a visually impaired person. Focus on the main obstacles or objects."
                                )
                            }

                            val response =
                                gemini.generateContent(input)

                            processingJob.cancel()

                            response.text?.let {
                                IrisEventBus.publish(
                                    IrisEvent.Speak(it)
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            IrisEventBus.publish(
                                IrisEvent.Speak(
                                    "Sorry, I could not analyze the scene"
                                )
                            )
                        } finally {
                            photoFile.delete()
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    scope.launch {
                        IrisEventBus.publish(

                            IrisEvent.Speak("Camera error")
                        )
                    }
                }
            }
        )
        AttentionController.release()
    }
}
