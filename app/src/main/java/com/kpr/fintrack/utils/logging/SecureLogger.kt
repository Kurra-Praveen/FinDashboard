package com.kpr.fintrack.utils.logging

import android.util.Log
import com.kpr.fintrack.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureLogger @Inject constructor() {
    init {
        Log.d("SecureLogger", "SecureLogger initialized")
    }

    fun d(tag: String, message: String) {
        if (BuildConfig.ENABLE_SECURE_LOGGING) {
            Log.d("FinTrack_$tag", sanitizeMessage(message))
        }
    }

    fun i(tag: String, message: String) {
        if (BuildConfig.ENABLE_SECURE_LOGGING) {
            Log.i("FinTrack_$tag", sanitizeMessage(message))
        }
    }

    fun w(tag: String, message: String) {
        if (BuildConfig.ENABLE_SECURE_LOGGING) {
            Log.w("FinTrack_$tag", sanitizeMessage(message))
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.ENABLE_SECURE_LOGGING) {
            Log.e("FinTrack_$tag", sanitizeMessage(message), throwable)
        }
    }

    private fun sanitizeMessage(message: String): String {
        if (!BuildConfig.ENABLE_SECURE_LOGGING) return "[REDACTED]"

        // In production builds, redact sensitive information
        return if (BuildConfig.DEBUG) {
            message
        } else {
            message
                .replace("\\b\\d{4}\\s*\\d{4}\\s*\\d{4}\\s*\\d{4}\\b".toRegex(), "[CARD_REDACTED]")
                .replace("\\b\\d{10,}\\b".toRegex(), "[NUMBER_REDACTED]")
                .replace("Rs\\.?\\s*\\d+(?:,\\d+)*(?:\\.\\d{2})?".toRegex(), "Rs.[AMOUNT_REDACTED]")
        }
    }
}
