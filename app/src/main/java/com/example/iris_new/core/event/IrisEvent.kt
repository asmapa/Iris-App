package com.example.iris_new.core.event

sealed class IrisEvent {

    // Continuous mode
    data class ObstacleDetected(
        val intensity: Float
    ) : IrisEvent()

    // On-command
    object DescribeScene : IrisEvent()
    object ReadText : IrisEvent()
    data class StartNavigation(val destination: String) : IrisEvent()

    // Emergency
    object EmergencyTriggered : IrisEvent()

    // Face system (NEW)
    data class FaceDetected(
        val embedding: FloatArray
    ) : IrisEvent()

    data class FaceRecognized(
        val name: String
    ) : IrisEvent()
    // Output
    data class Speak(val text: String) : IrisEvent()
}