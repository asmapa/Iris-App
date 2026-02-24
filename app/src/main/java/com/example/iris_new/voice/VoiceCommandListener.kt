package com.example.iris_new.voice

import com.example.iris_new.core.event.IrisEvent
import com.example.iris_new.core.event.IrisEventBus

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


        command.contains("instructions") || command.contains("what can you do") -> {
            IrisEventBus.publish(
                IrisEvent.Speak(
                    "You can say: read text, describe scene, help for emergency, navigate to a place, or teach a face."
                )
            )
        }



    }
}