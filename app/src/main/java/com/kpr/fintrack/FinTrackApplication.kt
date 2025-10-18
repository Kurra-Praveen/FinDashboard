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
            System.loadLibrary(getString(R.string.sqlcipher_lib_name))
            android.util.Log.d(getString(R.string.fintrack_application_tag), getString(R.string.log_sqlcipher_loaded_successfully))
        } catch (e: Exception) {
            android.util.Log.e(getString(R.string.fintrack_application_tag), getString(R.string.log_failed_to_load_sqlcipher), e)
        }

        // Initialize WorkManager with our custom configuration
        try {
            android.util.Log.d(getString(R.string.fintrack_application_tag), getString(R.string.log_initializing_workmanager))
            WorkManager.initialize(this, workManagerConfiguration)
            android.util.Log.d(getString(R.string.fintrack_application_tag), getString(R.string.log_workmanager_initialized))
        } catch (e: Exception) {
            android.util.Log.e(getString(R.string.fintrack_application_tag), getString(R.string.log_failed_to_initialize_workmanager), e)
        }

        // Initialize notification system with a delay to ensure Hilt is ready
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            initializeNotificationSystem()
        }, 100)
    }

    override val workManagerConfiguration: Configuration
        get() {
            Log.d(getString(R.string.fintrack_application_tag), getString(R.string.log_creating_workmanager_config))
            Log.d(getString(R.string.fintrack_application_tag), getString(R.string.log_workerfactory_initialized_status, ::workerFactory.isInitialized))
            if (::workerFactory.isInitialized) {
                Log.d(getString(R.string.fintrack_application_tag), getString(R.string.log_workerfactory_class_name, workerFactory::class.java.simpleName))
            }
            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        }

    private fun initializeNotificationSystem() {
        try {
            android.util.Log.d(getString(R.string.fintrack_application_tag), getString(R.string.log_initializing_notification_system))
            
            // Initialize notification channels
            notificationHelper.initializeNotificationChannels()
            
            // Schedule daily notifications if enabled
            if (notificationHelper.isDailySpendingNotificationEnabled()) {
                android.util.Log.d(getString(R.string.fintrack_application_tag), getString(R.string.log_daily_notifications_enabled))
                NotificationScheduler.scheduleDailyNotifications(this)
                android.util.Log.d(getString(R.string.fintrack_application_tag), getString(R.string.log_daily_spending_notifications_scheduled))
            } else {
                android.util.Log.d(getString(R.string.fintrack_application_tag), getString(R.string.log_daily_notifications_disabled))
            }
        } catch (e: Exception) {
            android.util.Log.e(getString(R.string.fintrack_application_tag), getString(R.string.log_failed_to_initialize_notification_system), e)
        }
    }
}
