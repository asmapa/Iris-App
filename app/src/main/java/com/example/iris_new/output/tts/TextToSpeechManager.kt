package com.example.iris_new.output.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.iris_new.core.event.IrisEvent
import com.example.iris_new.core.event.IrisEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

class TextToSpeechManager(
    context: Context,
    scope: CoroutineScope
) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context, this)

    init {
        scope.launch {
            IrisEventBus.events.collect { event ->
                if (event is IrisEvent.Speak) {
                    tts.speak(event.text, TextToSpeech.QUEUE_FLUSH, null, "")
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.speak("IRIS system ready", TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }
}