package com.example.iris_new.core.system

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class IrisNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        val packageName = sbn.packageName
        val extras = sbn.notification.extras

        val title = extras.getString("android.title")
        val text = extras.getString("android.text")
        val bigText = extras.getString("android.bigText")

        Log.d("IRIS_NOTIFICATION", "title=$title text=$text bigText=$bigText")

        val message = when (packageName) {

            "com.whatsapp" -> {

                val sender = title ?: "someone"

                "WhatsApp message from $sender"
            }

            else -> {

                val app = packageName.substringAfterLast(".")
                "$app notification"
            }
        }

        NotificationRepository.addNotification(message)
    }
}