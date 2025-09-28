package com.kpr.fintrack

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.kpr.fintrack.services.notification.NotificationScheduler
import com.kpr.fintrack.utils.notification.NotificationHelper
import com.kpr.fintrack.workers.CustomWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import net.zetetic.database.sqlcipher.SQLiteDatabase
import javax.inject.Inject

//import net.sqlcipher.database.SQLiteDatabase


@HiltAndroidApp
class FinTrackApplication: Application(), Configuration.Provider {

    @Inject
    lateinit var notificationHelper: NotificationHelper
    
    @Inject
    lateinit var workerFactory: CustomWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Initialize SQLCipher libraries early - CRITICAL for avoiding crashes
        try {
            System.loadLibrary("sqlcipher")
            android.util.Log.d("FinTrackApplication", "SQLCipher libraries loaded successfully")
        } catch (e: Exception) {
            android.util.Log.e("FinTrackApplication", "Failed to load SQLCipher libraries", e)
        }

        // Initialize WorkManager with our custom configuration
        try {
            android.util.Log.d("FinTrackApplication", "Initializing WorkManager with custom configuration")
            WorkManager.initialize(this, workManagerConfiguration)
            android.util.Log.d("FinTrackApplication", "WorkManager initialized with CustomWorkerFactory")
        } catch (e: Exception) {
            android.util.Log.e("FinTrackApplication", "Failed to initialize WorkManager", e)
        }

        // Initialize notification system with a delay to ensure Hilt is ready
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            initializeNotificationSystem()
        }, 100)
    }

    override val workManagerConfiguration: Configuration
        get() {
            Log.d("FinTrackApplication", "Creating WorkManager configuration with CustomWorkerFactory")
            Log.d("FinTrackApplication", "WorkerFactory is initialized: ${::workerFactory.isInitialized}")
            if (::workerFactory.isInitialized) {
                Log.d("FinTrackApplication", "WorkerFactory: ${workerFactory::class.java.simpleName}")
            }
            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        }

    private fun initializeNotificationSystem() {
        try {
            android.util.Log.d("FinTrackApplication", "Initializing notification system")
            
            // Initialize notification channels
            notificationHelper.initializeNotificationChannels()
            
            // Schedule daily notifications if enabled
            if (notificationHelper.isDailySpendingNotificationEnabled()) {
                android.util.Log.d("FinTrackApplication", "Daily notifications enabled, scheduling...")
                NotificationScheduler.scheduleDailyNotifications(this)
                android.util.Log.d("FinTrackApplication", "Daily spending notifications scheduled")
            } else {
                android.util.Log.d("FinTrackApplication", "Daily notifications disabled")
            }
        } catch (e: Exception) {
            android.util.Log.e("FinTrackApplication", "Failed to initialize notification system", e)
        }
    }
}
