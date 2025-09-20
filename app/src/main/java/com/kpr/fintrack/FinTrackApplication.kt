package com.kpr.fintrack

import android.app.Application
import android.util.Log

import dagger.hilt.android.HiltAndroidApp
import net.zetetic.database.sqlcipher.SQLiteDatabase

//import net.sqlcipher.database.SQLiteDatabase


@HiltAndroidApp
class FinTrackApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize SQLCipher libraries early - CRITICAL for avoiding crashes
        try {
            System.loadLibrary("sqlcipher")
            android.util.Log.d("FinTrackApplication", "SQLCipher libraries loaded successfully")
        } catch (e: Exception) {
            android.util.Log.e("FinTrackApplication", "Failed to load SQLCipher libraries", e)
        }
    }
}
