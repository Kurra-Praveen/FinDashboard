package com.kpr.fintrack

import android.app.Application
import android.util.Log

import dagger.hilt.android.HiltAndroidApp
import net.sqlcipher.database.SQLiteDatabase


@HiltAndroidApp
class FinTrackApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize SQLCipher
        SQLiteDatabase.loadLibs(this)

        Log.d("FinTrackApplication", "Application onCreate called")
    // TODO: Initialize crash reporting, analytics if needed
    }
}
