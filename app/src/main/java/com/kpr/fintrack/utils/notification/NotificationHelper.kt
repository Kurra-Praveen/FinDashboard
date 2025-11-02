package com.kpr.fintrack.utils.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.kpr.fintrack.utils.logging.SecureLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureLogger: SecureLogger
) {
    
//    companion object {
//        private const val DAILY_SPENDING_CHANNEL_ID = "daily_spending_channel"
//        private const val DAILY_SPENDING_CHANNEL_NAME = "Daily Spending Analytics"
//        private const val DAILY_SPENDING_CHANNEL_DESCRIPTION = "Daily spending summary and analytics notifications"
//    }

//    fun initializeNotificationChannels() {
//        createDailySpendingChannel()
//        secureLogger.d("NotificationHelper", "Notification channels initialized")
//    }

//    private fun createDailySpendingChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                DAILY_SPENDING_CHANNEL_ID,
//                DAILY_SPENDING_CHANNEL_NAME,
//                NotificationManager.IMPORTANCE_DEFAULT
//            ).apply {
//                description = DAILY_SPENDING_CHANNEL_DESCRIPTION
//                enableLights(true)
//                enableVibration(true)
//                setShowBadge(true)
//            }
//
//            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(channel)
//        }
//    }

//    fun areNotificationsEnabled(): Boolean {
//        return NotificationManagerCompat.from(context).areNotificationsEnabled()
//    }

//    fun isDailySpendingNotificationEnabled(): Boolean {
//        val prefs = context.getSharedPreferences("fintrack_prefs", Context.MODE_PRIVATE)
//        return prefs.getBoolean("daily_notifications_enabled", true)
//    }

//    fun setDailySpendingNotificationEnabled(enabled: Boolean) {
//        val prefs = context.getSharedPreferences("fintrack_prefs", Context.MODE_PRIVATE)
//        prefs.edit().putBoolean("daily_notifications_enabled", enabled).apply()
//
//        if (enabled) {
//            NotificationScheduler.scheduleDailyNotifications(context)
//            secureLogger.i("NotificationHelper", "Daily spending notifications enabled")
//        } else {
//            NotificationScheduler.cancelDailyNotifications(context)
//            secureLogger.i("NotificationHelper", "Daily spending notifications disabled")
//        }
//    }

//    fun getNotificationTimePreference(): String {
//        val prefs = context.getSharedPreferences("fintrack_prefs", Context.MODE_PRIVATE)
//        return prefs.getString("notification_time", "20:00") ?: "20:00"
//    }

//    fun setNotificationTimePreference(time: String) {
//        val prefs = context.getSharedPreferences("fintrack_prefs", Context.MODE_PRIVATE)
//        prefs.edit().putString("notification_time", time).apply()
//
//        // Reschedule notifications with new time
//        if (isDailySpendingNotificationEnabled()) {
//            NotificationScheduler.cancelDailyNotifications(context)
//            // Add a small delay to ensure cancellation completes
//            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
//                NotificationScheduler.scheduleDailyNotifications(context)
//            }, 100)
//        }
//    }

//    fun getNotificationFrequencyPreference(): String {
//        val prefs = context.getSharedPreferences("fintrack_prefs", Context.MODE_PRIVATE)
//        return prefs.getString("notification_frequency", "daily") ?: "daily"
//    }

//    fun setNotificationFrequencyPreference(frequency: String) {
//        val prefs = context.getSharedPreferences("fintrack_prefs", Context.MODE_PRIVATE)
//        prefs.edit().putString("notification_frequency", frequency).apply()
//
//        // Reschedule notifications with new frequency
//        if (isDailySpendingNotificationEnabled()) {
//            NotificationScheduler.cancelDailyNotifications(context)
//            NotificationScheduler.scheduleDailyNotifications(context)
//        }
//    }

//    fun getNotificationInsightsPreference(): Boolean {
//        val prefs = context.getSharedPreferences("fintrack_prefs", Context.MODE_PRIVATE)
//        return prefs.getBoolean("notification_insights_enabled", true)
//    }
//
//    fun setNotificationInsightsPreference(enabled: Boolean) {
//        val prefs = context.getSharedPreferences("fintrack_prefs", Context.MODE_PRIVATE)
//        prefs.edit().putBoolean("notification_insights_enabled", enabled).apply()
//    }
//
//    fun getNotificationComparisonPreference(): Boolean {
//        val prefs = context.getSharedPreferences("fintrack_prefs", Context.MODE_PRIVATE)
//        return prefs.getBoolean("notification_comparison_enabled", true)
//    }
//
//    fun setNotificationComparisonPreference(enabled: Boolean) {
//        val prefs = context.getSharedPreferences("fintrack_prefs", Context.MODE_PRIVATE)
//        prefs.edit().putBoolean("notification_comparison_enabled", enabled).apply()
//    }
//
//    fun getNotificationBudgetAlertsPreference(): Boolean {
//        val prefs = context.getSharedPreferences("fintrack_prefs", Context.MODE_PRIVATE)
//        return prefs.getBoolean("notification_budget_alerts_enabled", true)
//    }
//
//    fun setNotificationBudgetAlertsPreference(enabled: Boolean) {
//        val prefs = context.getSharedPreferences("fintrack_prefs", Context.MODE_PRIVATE)
//        prefs.edit().putBoolean("notification_budget_alerts_enabled", enabled).apply()
//    }

    data class NotificationSettings(
        val dailyNotificationsEnabled: Boolean,
        val notificationTime: String,
        val notificationFrequency: String,
        val insightsEnabled: Boolean,
        val comparisonEnabled: Boolean,
        val budgetAlertsEnabled: Boolean
    )
}
