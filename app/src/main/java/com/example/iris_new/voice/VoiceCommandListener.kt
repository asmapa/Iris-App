package com.example.iris_new.voice

import com.example.iris_new.core.event.IrisEvent
import com.example.iris_new.core.event.IrisEventBus
import android.util.Log

suspend fun processCommand(command: String) {
    when {
        command.contains("navigate to") || command.contains("go to") -> {
            val destination =
                command.substringAfter("navigate to")
                    .substringAfter("go to")
                    .trim()

            if (destination.isNotEmpty()) {
                IrisEventBus.publish(
                    IrisEvent.StartNavigation(destination)
                )
            }
        }


        command.contains("describe") ->
            IrisEventBus.publish(IrisEvent.DescribeScene)



        command.contains("read") ->
            IrisEventBus.publish(IrisEvent.ReadText)



        command.contains("help") ->
            IrisEventBus.publish(IrisEvent.EmergencyTriggered)



        // 🔥 ADD THIS BLOCK
        command.contains("how much") ||
                command.contains("currency") ||
                command.contains("how much is this") -> {

            IrisEventBus.publish(IrisEvent.DetectCurrency)
        }



        command.contains("recognize") -> {
            Log.d("FACE_DEBUG", "Recognize command detected")
            IrisEventBus.publish(IrisEvent.StartRecognition)
        }

        command.contains("back") ||
                command.contains("go back") ||
                command.contains("exit") -> {

            IrisEventBus.publish(IrisEvent.GoBack)


        }

        command.contains("instructions") || command.contains("what can you do") -> {
            IrisEventBus.publish(
                IrisEvent.Speak(
                    "You can say: read text, describe for screen description, help for emergency, navigate to a place if u want to go anywhere you want, how much to detect currency,phone status to know all notifications,command find with object name to detect particular object is in front of you or not and say exit if you want to stop find."
                )
            )
        }

        command.contains("stop") ||
                command.contains("cancel") -> {

            IrisEventBus.publish(IrisEvent.StopReading)

        }


        command.contains("phone status") ||
                command.contains("system status") ||
                command.contains("device status") -> {

            IrisEventBus.publish(
                IrisEvent.PhoneStatus
            )
        }


        command.contains("color") ||
                command.contains("detect color") ||
                command.contains("what color") -> {

            if ("color" in command) {

                Log.d("COLOR_DEBUG", "Color command detected")

                IrisEventBus.publish(IrisEvent.DetectColor)

            }
        }


        command.startsWith("find ") -> {

            val target = command.substringAfter("find ").trim()

            if (target.isNotEmpty()) {

                IrisEventBus.publish(
                    IrisEvent.StartFind(target)
                )
            }
        }



    }
}