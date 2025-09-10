package com.kpr.fintrack.utils.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseKeyProvider @Inject constructor(
    private val context: Context
) {
        init {
            android.util.Log.d("DatabaseKeyProvider", "Provider initialized")
        }

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "fintrack_secure_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getDatabaseKey(): ByteArray {
        android.util.Log.d("DatabaseKeyProvider", "getDatabaseKey called")
        val existingKey = sharedPreferences.getString(DATABASE_KEY, null)

        return if (existingKey != null) {
            existingKey.toByteArray()
        } else {
            val newKey = generateRandomKey()
            sharedPreferences.edit().putString(DATABASE_KEY, String(newKey)).apply()
            newKey
        }
    }

    private fun generateRandomKey(): ByteArray {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()"
        return (1..32)
            .map { chars.random() }
            .joinToString("")
            .toByteArray()
    }

    companion object {
        private const val DATABASE_KEY = "database_key"
    }
}
