package com.example.iris_new.core.caretaker

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class CaretakerManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("caretaker_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PRIMARY = "caretaker_primary"
        private const val BACKUP = "caretaker_backup"
        private const val TAG = "CaretakerManager"
    }

    // Format phone number
    private fun formatNumber(input: String): String {
        val trimmed = input.replace(" ", "")
        return when {
            trimmed.startsWith("+") -> trimmed
            trimmed.length == 10 -> "+91$trimmed"
            else -> trimmed
        }
    }

    // Validate phone
    private fun isValidNumber(number: String): Boolean {
        return number.length >= 10
    }

    // Add numbers first time
    fun addCaretakerNumber(number: String): Result<String> {
        val formatted = formatNumber(number)

        if (!isValidNumber(formatted)) {
            return Result.failure(Exception("Invalid phone number"))
        }

        return when {
            getPrimaryNumber() == null -> {
                prefs.edit().putString(PRIMARY, formatted).apply()
                logState()
                Result.success("Primary caretaker added")
            }

            getBackupNumber() == null -> {
                prefs.edit().putString(BACKUP, formatted).apply()
                logState()
                Result.success("Backup caretaker added")
            }

            else -> {
                Result.failure(Exception("Both caretaker numbers already exist"))
            }
        }
    }

    fun updatePrimary(number: String): Result<String> {
        val formatted = formatNumber(number)

        if (!isValidNumber(formatted))
            return Result.failure(Exception("Invalid phone number"))

        prefs.edit().putString(PRIMARY, formatted).apply()
        logState()
        return Result.success("Primary updated")
    }

    fun updateBackup(number: String): Result<String> {
        val formatted = formatNumber(number)

        if (!isValidNumber(formatted))
            return Result.failure(Exception("Invalid phone number"))

        prefs.edit().putString(BACKUP, formatted).apply()
        logState()
        return Result.success("Backup updated")
    }

    fun getPrimaryNumber(): String? =
        prefs.getString(PRIMARY, null)

    fun getBackupNumber(): String? =
        prefs.getString(BACKUP, null)

    fun getEmergencyNumber(): String? =
        getPrimaryNumber() ?: getBackupNumber()

    fun clearAll() {
        prefs.edit().clear().apply()
        Log.d(TAG, "All caretaker numbers cleared")
    }

    private fun logState() {
        Log.d(TAG, "Primary = ${getPrimaryNumber()}")
        Log.d(TAG, "Backup = ${getBackupNumber()}")
    }
}