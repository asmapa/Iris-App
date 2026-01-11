package com.example.iris_new.ui

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import com.example.iris_new.core.caretaker.CaretakerManager
import com.example.iris_new.databinding.ActivitySettingsBinding
import java.util.Locale

class SettingsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var caretakerManager: CaretakerManager
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        caretakerManager = CaretakerManager(this)
        tts = TextToSpeech(this, this)

        binding.addCaretakerButton.setOnClickListener {
            val number =
                binding.caretakerPhone.text.toString().trim()

            if (number.isNotEmpty()) {
                caretakerManager.addCaretakerNumber(number)
                binding.caretakerPhone.text.clear()
                speak("Caretaker number saved")
            }
        }

        binding.aboutButton.setOnClickListener {
            startActivity(
                android.content.Intent(
                    this,
                    About::class.java
                )
            )
        }

        binding.button2.setOnClickListener {
            startActivity(
                Intent(this, TeachFaceActivity::class.java)
            )
        }

    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            speak("Settings opened")
        }
    }

    private fun speak(text: String) {
        tts.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            null
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
    }
}
