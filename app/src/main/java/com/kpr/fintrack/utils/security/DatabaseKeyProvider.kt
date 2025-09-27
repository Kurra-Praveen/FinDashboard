package com.kpr.fintrack.utils.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit
import javax.crypto.AEADBadTagException
import android.security.keystore.KeyPermanentlyInvalidatedException
import java.security.GeneralSecurityException
import java.io.IOException

@Singleton
class DatabaseKeyProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var _sharedPreferences: SharedPreferences? = null

    init {
        android.util.Log.d("DatabaseKeyProvider", "Provider initialized")
    }

    private val sharedPreferences: SharedPreferences
        get() {
            if (_sharedPreferences == null) {
                _sharedPreferences = createEncryptedSharedPreferences()
            }
            return _sharedPreferences!!
        }

    private fun createEncryptedSharedPreferences(): SharedPreferences {
        return try {
            val masterKeyAlias = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "fintrack_secure_prefs",
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            android.util.Log.w("DatabaseKeyProvider", "Failed to create encrypted preferences, attempting recovery", e)
            handleEncryptedPreferencesError(e)
        }
    }

    private fun handleEncryptedPreferencesError(originalException: Exception): SharedPreferences {
        return when (originalException) {
            is AEADBadTagException,
            is KeyPermanentlyInvalidatedException,
            is GeneralSecurityException -> {
                android.util.Log.i("DatabaseKeyProvider", "Keystore key invalidated, clearing preferences and recreating")

                // Clear the corrupted preferences
                clearCorruptedPreferences()

                // Try to recreate with a new master key
                try {
                    val masterKeyAlias = MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()

                    EncryptedSharedPreferences.create(
                        context,
                        "fintrack_secure_prefs",
                        masterKeyAlias,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                } catch (retryException: Exception) {
                    android.util.Log.e("DatabaseKeyProvider", "Failed to recreate encrypted preferences", retryException)
                    // Fall back to regular shared preferences as last resort
                    createFallbackPreferences()
                }
            }
            else -> {
                android.util.Log.e("DatabaseKeyProvider", "Unexpected error creating encrypted preferences", originalException)
                createFallbackPreferences()
            }
        }
    }

    private fun clearCorruptedPreferences() {
        try {
            // Clear the shared preferences file
            val prefsFile = context.getSharedPreferences("fintrack_secure_prefs", Context.MODE_PRIVATE)
            prefsFile.edit { clear() }

            // Also clear any database that might be encrypted with the old key
            clearKeyAndDatabase()
        } catch (e: Exception) {
            android.util.Log.w("DatabaseKeyProvider", "Error clearing corrupted preferences", e)
        }
    }

    private fun createFallbackPreferences(): SharedPreferences {
        android.util.Log.w("DatabaseKeyProvider", "Using fallback unencrypted preferences")
        return context.getSharedPreferences("fintrack_fallback_prefs", Context.MODE_PRIVATE)
    }

    fun getDatabaseKey(): ByteArray {
        android.util.Log.d("DatabaseKeyProvider", "getDatabaseKey called")

        return try {
            val existingKey = sharedPreferences.getString(DATABASE_KEY, null)

            if (existingKey != null) {
                existingKey.toByteArray()
            } else {
                val newKey = generateRandomKey()
                sharedPreferences.edit {
                    putString(DATABASE_KEY, String(newKey))
                }
                newKey
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseKeyProvider", "Error getting database key", e)
            // Reset and try again
            _sharedPreferences = null
            clearKeyAndDatabase()

            val newKey = generateRandomKey()
            try {
                sharedPreferences.edit {
                    putString(DATABASE_KEY, String(newKey))
                }
            } catch (saveException: Exception) {
                android.util.Log.e("DatabaseKeyProvider", "Failed to save new key", saveException)
            }
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

    fun clearKeyAndDatabase() {
        android.util.Log.d("DatabaseKeyProvider", "Clearing key and database")

        try {
            _sharedPreferences?.edit { remove(DATABASE_KEY) }
        } catch (e: Exception) {
            android.util.Log.w("DatabaseKeyProvider", "Error clearing key from preferences", e)
        }

        // Delete database file if it exists
        try {
            val dbFile = context.getDatabasePath(com.kpr.fintrack.BuildConfig.DATABASE_NAME)
            if (dbFile.exists()) {
                val deleted = dbFile.delete()
                android.util.Log.d("DatabaseKeyProvider", "Database file deleted: $deleted")
            }
        } catch (e: Exception) {
            android.util.Log.w("DatabaseKeyProvider", "Error deleting database file", e)
        }
    }

    companion object {
        private const val DATABASE_KEY = "database_key"
    }
}