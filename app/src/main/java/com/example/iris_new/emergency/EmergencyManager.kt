package com.example.iris_new.emergency

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.example.iris_new.core.event.IrisEvent
import com.example.iris_new.core.event.IrisEventBus
import com.example.iris_new.core.state.AttentionController
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.example.iris_new.core.caretaker.CaretakerManager

class EmergencyManager(
    private val context: Context,
    private val scope: CoroutineScope
) {

    private val caretakerManager = CaretakerManager(context)

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val handler = Handler(Looper.getMainLooper())

    init {
        scope.launch {
            IrisEventBus.events.collect { event ->
                if (event is IrisEvent.EmergencyTriggered) {
                    handleEmergency()
                }
            }
        }
    }

    private fun handleEmergency() {

        AttentionController.lock()

        scope.launch {
            IrisEventBus.publish(
                IrisEvent.Speak("Emergency detected. Contacting caretakers.")
            )
        }

        val primary = caretakerManager.getPrimaryNumber()
        val backup = caretakerManager.getBackupNumber()

        if (primary == null && backup == null) {
            scope.launch {
                IrisEventBus.publish(
                    IrisEvent.Speak(
                        "No caretaker number saved. Please add one in settings."
                    )
                )
            }
            AttentionController.release()
            return
        }

        if (
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {

            sendSms(primary, "Emergency! I need help. Location unavailable.")
            sendSms(backup, "Emergency! I need help. Location unavailable.")

            startPrimaryCall(primary, backup)
            return
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->

                    val message = buildMessage(location)

                    sendSms(primary, message)
                    sendSms(backup, message)

                    startPrimaryCall(primary, backup)
                }
                .addOnFailureListener {

                    sendSms(primary, "Emergency! I need help. Location unavailable.")
                    sendSms(backup, "Emergency! I need help. Location unavailable.")

                    startPrimaryCall(primary, backup)
                }

        } catch (e: SecurityException) {

            sendSms(primary, "Emergency! I need help. Location unavailable.")
            sendSms(backup, "Emergency! I need help. Location unavailable.")

            startPrimaryCall(primary, backup)
        }
    }

    private fun buildMessage(location: Location?): String {
        return if (location != null) {
            val link =
                "https://maps.google.com/?q=${location.latitude},${location.longitude}"
            "Emergency! I need help. My location: $link"
        } else {
            "Emergency! I need help. Location unavailable."
        }
    }

    private fun sendSms(number: String?, message: String) {

        if (number == null) return

        if (
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val sms = SmsManager.getDefault()
        sms.sendTextMessage(number, null, message, null, null)
    }

    private fun startPrimaryCall(primary: String?, backup: String?) {

        if (primary == null) {
            makeCall(backup)
            return
        }

        // Call primary
        makeCall(primary)

        // Wait 20 seconds
        handler.postDelayed({

            // After 20s call backup automatically
            if (backup != null) {
                scope.launch {
                    IrisEventBus.publish(
                        IrisEvent.Speak("Primary not responding. Calling backup contact.")
                    )
                }
                makeCall(backup)
            }

        }, 20000) // 20 seconds delay
    }

    private fun makeCall(number: String?) {

        if (number == null) {
            AttentionController.release()
            return
        }

        if (
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            scope.launch {
                IrisEventBus.publish(
                    IrisEvent.Speak("Call permission not granted")
                )
            }
            AttentionController.release()
            return
        }

        try {
            val intent = Intent(
                Intent.ACTION_CALL,
                "tel:$number".toUri()
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(intent)

        } catch (e: SecurityException) {
            scope.launch {
                IrisEventBus.publish(
                    IrisEvent.Speak("Unable to place call")
                )
            }
        } finally {
            AttentionController.release()
        }
    }
}