package com.example.iris_new.face

import com.example.iris_new.core.event.IrisEvent
import com.example.iris_new.core.event.IrisEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class FaceRecognitionManager(
    private val repository: FaceRepository,
    private val scope: CoroutineScope
) {

    // 🔒 control when recognition should run
    var recognitionEnabled = true

    // 🔊 speech cooldown tracking
    private var lastSpoken: String? = null
    private var lastTime = 0L

    // 🎯 confidence threshold (tune later)
    private val MATCH_THRESHOLD = 1.0f

    init {
        scope.launch {
            IrisEventBus.events.collect { event ->
                if (event is IrisEvent.FaceDetected && recognitionEnabled) {
                    recognize(event.embedding)
                }
            }
        }
    }

    private fun recognize(embedding: FloatArray) {

        scope.launch {

            val result: Pair<String, Float>? =
                repository.findMatchWithScore(embedding)

            val now = System.currentTimeMillis()

            // ---------------- UNKNOWN PERSON ----------------
            if (result == null) {

                // cooldown
                if (lastSpoken == "unknown" && now - lastTime < 5000) return@launch

                lastSpoken = "unknown"
                lastTime = now

                IrisEventBus.publish(
                    IrisEvent.Speak("Unknown person")
                )
                return@launch
            }

            val name = result.first
            val score = result.second

            // ---------------- CONFIDENCE CHECK ----------------
            if (score > MATCH_THRESHOLD) {

                if (lastSpoken == "unknown" && now - lastTime < 5000) return@launch

                lastSpoken = "unknown"
                lastTime = now

                IrisEventBus.publish(
                    IrisEvent.Speak("Unknown person")
                )
                return@launch
            }

            // ---------------- SAME PERSON COOLDOWN ----------------
            if (name == lastSpoken && now - lastTime < 10_000) return@launch

            lastSpoken = name
            lastTime = now

            IrisEventBus.publish(
                IrisEvent.Speak("$name is in front of you")
            )
        }
    }
}
