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

    // 🔊 continuous MLKit face tracking
    private var activeTrackingId: Int? = null
    private var activeIdentity: String? = null
    private var lastSpokenTime = 0L

    // 🎯 confidence threshold (aligned with FaceRepository)
    private val MATCH_THRESHOLD = 1.05f

    init {
        scope.launch {
            IrisEventBus.events.collect { event ->
                if (event is IrisEvent.FaceDetected && recognitionEnabled) {
                    recognize(event.embedding, event.trackingId)
                }
            }
        }
    }

    private fun recognize(embedding: FloatArray, trackingId: Int?) {

        scope.launch {

            val result = repository.findMatchWithScore(embedding)
            val now = System.currentTimeMillis()

            val isUnknown = result == null || result.second > MATCH_THRESHOLD
            val identity = if (isUnknown) "unknown" else result!!.first

            // Did a physically new face enter the frame?
            val isNewFace = (trackingId != null && trackingId != activeTrackingId)

            if (isNewFace || trackingId == null) {

                // Fallback debounce for untracked frames
                if (trackingId == null && now - lastSpokenTime < 5000) return@launch

                activeTrackingId = trackingId
                activeIdentity = identity
                lastSpokenTime = now

                val msg = "$identity is in front of you"
                IrisEventBus.publish(IrisEvent.Speak(msg))
                return@launch
            }

            // Same physical person is still in frame
            // Prevent spam from wiggles/lighting changes. Only re-announce every 10s.
            if (now - lastSpokenTime > 10_000) {
                activeIdentity = identity
                lastSpokenTime = now

                val msg = "$identity is in front of you"
                IrisEventBus.publish(IrisEvent.Speak(msg))
            }
        }
    }
}
