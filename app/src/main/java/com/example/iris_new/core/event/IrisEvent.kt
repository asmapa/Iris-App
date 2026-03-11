package com.example.iris_new.core.event
import android.graphics.Rect
import android.graphics.Bitmap
sealed class IrisEvent {

    // Continuous mode
    data class TeachingFaceCaptured(val bitmap: Bitmap) : IrisEvent()
    data class ObstacleDetected(
        val intensity: Float
    ) : IrisEvent()


    data class FaceDetected(
        val embedding: FloatArray,
        val trackingId: Int? = null
    ) : IrisEvent()
    object StartRecognition : IrisEvent()
    object OpenObjectDetection : IrisEvent()
    data class FaceBox(
        val rect: Rect,
        val imageWidth: Int,
        val imageHeight: Int
    ) : IrisEvent()

    data class TeachingEmbedding(
        val embedding: FloatArray,
        val yaw: Float,
        val pitch: Float
    ) : IrisEvent()
    object DetectColor : IrisEvent()

    object DetectCurrency : IrisEvent()

    // 🔎 start searching
    data class StartFind(
        val target: String
    ) : IrisEvent()

    object GoBack : IrisEvent()

    object StopReading : IrisEvent()

    // 📦 object detected from model
    data class ObjectDetected(
        val label: String,
        val confidence: Float
    ) : IrisEvent()

    data object PhoneStatus : IrisEvent()

    // On-command
    object DescribeScene : IrisEvent()
    object ReadText : IrisEvent()
    data class StartNavigation(val destination: String) : IrisEvent()

    // Emergency
    object EmergencyTriggered : IrisEvent()

    // Face system (NEW)


    data class FaceRecognized(
        val name: String
    ) : IrisEvent()
    // Output
    data class Speak(val text: String) : IrisEvent()




}