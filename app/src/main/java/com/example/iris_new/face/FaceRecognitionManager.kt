package com.example.iris_new.face

import com.example.iris_new.core.event.IrisEvent
import com.example.iris_new.core.event.IrisEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class FaceRecognitionManager(
    private val repository: FaceRepository,
    private val scope: CoroutineScope
) {

    private var lastSpoken: String? = null
    private var lastTime = 0L

    init {
        scope.launch {
            IrisEventBus.events.collect { event ->
                if (event is IrisEvent.FaceDetected) {
                    recognize(event.embedding)
                }
            }
        }
    }

    private fun recognize(embedding: FloatArray) {
        scope.launch {
            val name = repository.findMatch(embedding) ?: return@launch

            val now = System.currentTimeMillis()
            if (name == lastSpoken && now - lastTime < 10_000) return@launch

            lastSpoken = name
            lastTime = now

            IrisEventBus.publish(
                IrisEvent.Speak("$name is in front of you")
            )
        }
    }
}
