package com.example.iris_new.core.system

object NotificationRepository {

    val notifications = mutableListOf<String>()

    fun addNotification(message: String) {

        notifications.add(message)

        if (notifications.size > 10) {
            notifications.removeAt(0)
        }
    }

    fun getSummary(): String {

        if (notifications.isEmpty()) {
            return "No recent notifications"
        }

        return buildString {

            append("You have ${notifications.size} notifications. ")

            notifications.takeLast(3).forEach {
                append("$it. ")
            }
        }
    }
}