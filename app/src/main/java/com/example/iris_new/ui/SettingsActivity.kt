package com.example.iris_new.ui

import android.os.Bundle

import com.example.iris_new.databinding.ActivitySettingsBinding
import com.example.iris_new.core.caretaker.CaretakerManager
import com.example.iris_new.ui.TeachFaceActivity

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import java.util.Locale
import android.content.Intent






class SettingsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var caretakerManager: CaretakerManager

    private lateinit var tts: TextToSpeech

    // Flags for edit mode
    private var isEditingBackup = false
    private var isEditingPrimary = false





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        caretakerManager = CaretakerManager(this)
        // 1️⃣ Bind layout
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2️⃣ Initialize TTS
        tts = TextToSpeech(this, this)



        //BINDING TEACH FACE BUTTON
        binding.teachFaceButton.setOnClickListener {

            val intent = Intent(this, TeachFaceActivity::class.java)
            startActivity(intent)
        }



        binding.addCaretakerButton.setOnClickListener {

            val number = binding.caretakerPhone.text.toString().trim()

            if (number.isEmpty()) {
                toast("Enter phone number")
                return@setOnClickListener
            }

            caretakerManager.addCaretakerNumber(number)
                .onSuccess {
                    toast(it)
                    binding.caretakerPhone.text.clear()
                }
                .onFailure {
                    toast(it.message ?: "Failed to add caretaker")
                }
        }



        binding.editCaretakerButton.setOnClickListener {

            if (isEditingPrimary) {
                toast("Finish editing primary first")
                return@setOnClickListener
            }

            if (!isEditingBackup) {

                val savedBackup = caretakerManager.getBackupNumber()

                if (savedBackup != null) {
                    binding.caretakerPhone.setText(savedBackup)
                    binding.caretakerPhone.setSelection(savedBackup.length)
                    toast("Editing backup. Press again to save.")
                    isEditingBackup = true
                } else {
                    toast("No backup caretaker saved")
                }

            } else {

                val updated = binding.caretakerPhone.text.toString().trim()

                if (updated.isEmpty()) {
                    toast("Enter phone number")
                    return@setOnClickListener
                }

                caretakerManager.updateBackup(updated)
                    .onSuccess {
                        toast("Backup updated")
                        binding.caretakerPhone.text.clear()
                        isEditingBackup = false
                    }
                    .onFailure {
                        toast(it.message ?: "Update failed")
                    }
            }
        }


        binding.editCaretakerButton.setOnLongClickListener {

            if (isEditingBackup) {
                toast("Finish editing backup first")
                return@setOnLongClickListener true
            }

            if (!isEditingPrimary) {

                val savedPrimary = caretakerManager.getPrimaryNumber()

                if (savedPrimary != null) {
                    binding.caretakerPhone.setText(savedPrimary)
                    binding.caretakerPhone.setSelection(savedPrimary.length)
                    toast("Editing primary. Press again to save.")
                    isEditingPrimary = true
                } else {
                    toast("No primary caretaker saved")
                }

            } else {

                val updated = binding.caretakerPhone.text.toString().trim()

                if (updated.isEmpty()) {
                    toast("Enter phone number")
                    return@setOnLongClickListener true
                }

                caretakerManager.updatePrimary(updated)
                    .onSuccess {
                        toast("Primary updated")
                        binding.caretakerPhone.text.clear()
                        isEditingPrimary = false
                    }
                    .onFailure {
                        toast(it.message ?: "Update failed")
                    }
            }

            true
        }




        /* ---------------- ABOUT ---------------- */
        binding.aboutButton.setOnClickListener {
            val intent = Intent(this, About::class.java)
            startActivity(intent)
        }
    }



    fun Context.toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }



    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.speak(
                "Settings screen opened",
                TextToSpeech.QUEUE_FLUSH,
                null,
                null
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
    }
}
