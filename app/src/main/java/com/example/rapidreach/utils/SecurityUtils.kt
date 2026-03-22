package com.example.rapidreach.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurityUtils {
    private const val PREFS_NAME = "security_prefs"
    private const val PIN_KEY = "sos_pin"

    private fun getEncryptedPrefs(context: Context): android.content.SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            e.printStackTrace()
            context.getSharedPreferences("sos_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    fun savePin(context: Context, pin: String) {
        try {
            getEncryptedPrefs(context).edit().putString(PIN_KEY, pin).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getPin(context: Context): String? {
        return try {
            getEncryptedPrefs(context).getString(PIN_KEY, null)
        } catch (e: Exception) {
            null
        }
    }

    fun isPinSet(context: Context): Boolean {
        return getPin(context) != null
    }

    fun verifyPin(context: Context, enteredPin: String): Boolean {
        return getPin(context) == enteredPin
    }
}
