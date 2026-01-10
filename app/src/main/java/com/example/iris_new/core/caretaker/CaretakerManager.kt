package com.example.iris_new.core.caretaker

import android.content.Context
import android.content.SharedPreferences

class CaretakerManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(
            "caretaker_prefs",
            Context.MODE_PRIVATE
        )

    companion object {
        private const val PRIMARY = "caretaker_primary"
        private const val BACKUP = "caretaker_backup"
    }
    private fun formatNumber(input: String): String {
        val trimmed = input.replace(" ", "")

        return when {
            trimmed.startsWith("+") -> trimmed
            trimmed.length == 10 -> "+91$trimmed"   // Default India
            else -> trimmed
        }
    }

    fun addCaretakerNumber(number: String) {
        when {
            getPrimary() == null ->
                prefs.edit().putString(PRIMARY, number).apply()

            getBackup() == null ->
                prefs.edit().putString(BACKUP, number).apply()
        }
    }

    fun updatePrimary(number: String) {
        prefs.edit().putString(PRIMARY, number).apply()
    }

    fun updateBackup(number: String) {
        prefs.edit().putString(BACKUP, number).apply()
    }

    fun getPrimaryNumber(): String? = getPrimary()
    fun getBackupNumber(): String? = getBackup()

    fun getEmergencyNumber(): String? =
        getPrimary() ?: getBackup()

    private fun getPrimary(): String? =
        prefs.getString(PRIMARY, null)

    private fun getBackup(): String? =
        prefs.getString(BACKUP, null)
}
