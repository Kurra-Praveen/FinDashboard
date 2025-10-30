package com.kpr.fintrack

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.work.Configuration
import androidx.work.WorkManager
import com.kpr.fintrack.services.notification.NotificationScheduler
import com.kpr.fintrack.utils.notification.NotificationHelper
import com.kpr.fintrack.workers.CustomWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FinTrackApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var workerFactory: CustomWorkerFactory

    companion object {
        private const val PREFS_NAME = "fintrack_prefs"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize SQLCipher libraries
        try {
            System.loadLibrary(getString(R.string.sqlcipher_lib_name))
            Log.d(
                getString(R.string.fintrack_application_tag),
                getString(R.string.log_sqlcipher_loaded_successfully)
            )
        } catch (e: Exception) {
            Log.e(
                getString(R.string.fintrack_application_tag),
                getString(R.string.log_failed_to_load_sqlcipher),
                e
            )
        }

        // Initialize WorkManager
        try {
            Log.d(
                getString(R.string.fintrack_application_tag),
                getString(R.string.log_initializing_workmanager)
            )
            WorkManager.initialize(this, workManagerConfiguration)
            Log.d(
                getString(R.string.fintrack_application_tag),
                getString(R.string.log_workmanager_initialized)
            )
        } catch (e: Exception) {
            Log.e(
                getString(R.string.fintrack_application_tag),
                getString(R.string.log_failed_to_initialize_workmanager),
                e
            )
        }

        // Initialize notification system
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            initializeNotificationSystem()
        }, 100)
    }

    override val workManagerConfiguration: Configuration
        get() {
            Log.d(
                getString(R.string.fintrack_application_tag),
                getString(R.string.log_creating_workmanager_config)
            )
            Log.d(
                getString(R.string.fintrack_application_tag), getString(
                    R.string.log_workerfactory_initialized_status, ::workerFactory.isInitialized
                )
            )
            if (::workerFactory.isInitialized) {
                Log.d(
                    getString(R.string.fintrack_application_tag), getString(
                        R.string.log_workerfactory_class_name, workerFactory::class.java.simpleName
                    )
                )
            }
            return Configuration.Builder().setWorkerFactory(workerFactory).build()
        }

    private fun initializeNotificationSystem() {
        try {
            Log.d(
                getString(R.string.fintrack_application_tag),
                getString(R.string.log_initializing_notification_system)
            )
            // Initialize notification channels
            notificationHelper.initializeNotificationChannels()
            // --- THIS IS THE FIX ---
            // We only schedule on the very first launch.
            // After that, NotificationHelper is responsible for all scheduling.
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true)

            if (isFirstLaunch && notificationHelper.isDailySpendingNotificationEnabled()) {
                Log.d(
                    getString(R.string.fintrack_application_tag),
                    "First launch: Scheduling default daily notifications."
                )
                NotificationScheduler.scheduleDailyNotifications(this)
                prefs.edit { putBoolean(KEY_FIRST_LAUNCH, false) }
            } else if (notificationHelper.isDailySpendingNotificationEnabled()) {
                Log.d(
                    getString(R.string.fintrack_application_tag),
                    "App restart: Notifications are enabled, but we will NOT reschedule. WorkManager will handle the existing schedule."
                )
            } else {
                Log.d(
                    getString(R.string.fintrack_application_tag),
                    getString(R.string.log_daily_notifications_disabled)
                )
            }
        } catch (e: Exception) {
            Log.e(
                getString(R.string.fintrack_application_tag),
                getString(R.string.log_failed_to_initialize_notification_system),
                e
            )
        }
    }
}
