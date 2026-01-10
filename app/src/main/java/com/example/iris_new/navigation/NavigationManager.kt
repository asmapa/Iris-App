package com.example.iris_new.navigation

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.example.iris_new.core.event.IrisEvent
import com.example.iris_new.core.event.IrisEventBus
import com.example.iris_new.core.state.AttentionController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NavigationManager(
    private val context: Context,
    private val scope: CoroutineScope
) {

    init {
        scope.launch {
            IrisEventBus.events.collect { event ->
                if (event is IrisEvent.StartNavigation) {
                    startNavigation(event.destination)
                }
            }
        }
    }

    private fun startNavigation(destination: String) {
        // 🔒 Lock attention immediately
        AttentionController.lock()

        // 🗣️ Speak feedback (INSIDE coroutine)
        scope.launch {
            IrisEventBus.publish(
                IrisEvent.Speak(
                    "Starting navigation to $destination"
                )
            )
        }

        val uri =
            "google.navigation:q=$destination&mode=w".toUri()

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            scope.launch {
                IrisEventBus.publish(
                    IrisEvent.Speak(
                        "Google Maps is not available"
                    )
                )
            }
        }

        // 🔓 Release attention immediately
        AttentionController.release()
    }
}
