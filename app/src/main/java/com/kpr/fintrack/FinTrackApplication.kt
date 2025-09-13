package com.kpr.fintrack

import android.app.Application
import android.util.Log

import dagger.hilt.android.HiltAndroidApp
import net.sqlcipher.database.SQLiteDatabase


@HiltAndroidApp
class FinTrackApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize SQLCipher libraries early - CRITICAL for avoiding crashes
        try {
            SQLiteDatabase.loadLibs(this)
            android.util.Log.d("FinTrackApplication", "SQLCipher libraries loaded successfully")
        } catch (e: Exception) {
            android.util.Log.e("FinTrackApplication", "Failed to load SQLCipher libraries", e)
        }
    }
}
