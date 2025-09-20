package com.kpr.fintrack.utils.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class DatabaseKeyProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
        init {
            android.util.Log.d("DatabaseKeyProvider", "Provider initialized")
        }

    private val masterKeyAlias = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "fintrack_secure_prefs",
        masterKeyAlias,
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
            sharedPreferences.edit { putString(DATABASE_KEY, String(newKey)) }
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

    // Add this method to your existing DatabaseKeyProvider class
    fun clearKeyAndDatabase() {
        android.util.Log.d("DatabaseKeyProvider", "Clearing key and database")
        sharedPreferences.edit { remove(DATABASE_KEY) }

        // Delete database file if it exists
        val dbFile = context.getDatabasePath(com.kpr.fintrack.BuildConfig.DATABASE_NAME)
        if (dbFile.exists()) {
            val deleted = dbFile.delete()
            android.util.Log.d("DatabaseKeyProvider", "Database file deleted: $deleted")
        }
    }

}
