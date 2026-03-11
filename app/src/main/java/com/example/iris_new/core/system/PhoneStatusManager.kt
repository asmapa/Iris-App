package com.example.iris_new.core.system

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import com.example.iris_new.core.event.IrisEvent
import com.example.iris_new.core.event.IrisEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*

class PhoneStatusManager(
    private val context: Context,
    private val scope: CoroutineScope
) {

    init {

        scope.launch {

            IrisEventBus.events.collect { event ->

                if (event is IrisEvent.PhoneStatus) {
                    provideStatus()
                }
            }
        }
    }

    private suspend fun provideStatus() {

        val time = getCurrentTime()
        val battery = getBatteryLevel()
        val notifications = NotificationRepository.getSummary()

        val message = buildString {

            append("The time is $time. ")
            append("Battery level is $battery percent. ")
            append(notifications)
        }

        IrisEventBus.publish(
            IrisEvent.Speak(message)
        )
    }

    private fun getCurrentTime(): String {

        return DateFormat.format(
            "hh:mm a",
            Date()
        ).toString()
    }

    private fun getBatteryLevel(): Int {

        val batteryStatus = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        return batteryStatus?.getIntExtra(
            BatteryManager.EXTRA_LEVEL,
            -1
        ) ?: -1
    }
}