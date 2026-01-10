package com.example.iris_new.output.haptic

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.iris_new.core.event.IrisEvent
import com.example.iris_new.core.event.IrisEventBus
import com.example.iris_new.core.state.AttentionController
import com.example.iris_new.core.state.AttentionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class HapticManager(
    private val vibrator: Vibrator,
    scope: CoroutineScope
) {

    init {
        scope.launch {
            IrisEventBus.events.collect { event ->
                if (
                    event is IrisEvent.ObstacleDetected &&
                    AttentionController.state.value == AttentionState.FREE
                ) {
                    vibrate(event.intensity)
                }
            }
        }
    }

    private fun vibrate(intensity: Float) {
        val duration = (intensity * 200).toLong()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    duration,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }
}
