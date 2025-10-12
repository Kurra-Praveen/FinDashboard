package com.kpr.fintrack.services.notification

import android.content.Context
import androidx.work.*
import com.kpr.fintrack.utils.logging.SecureLogger
import java.util.concurrent.TimeUnit

class NotificationScheduler {

    companion object {
        fun scheduleDailyNotifications(context: Context) {
            val workManager = WorkManager.getInstance(context)
            android.util.Log.d("NotificationScheduler", "Scheduling daily notifications with WorkManager")

            // Cancel existing work first
            workManager.cancelUniqueWork("daily_spending_notification")
            workManager.cancelUniqueWork("daily_spending_recurring")

            // Create constraints for the notification
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .build()

            // Schedule a test worker first to verify Hilt WorkerFactory is working
            val testRequest = OneTimeWorkRequestBuilder<TestWorker>()
                .setConstraints(constraints)
                .setInitialDelay(5000, java.util.concurrent.TimeUnit.MILLISECONDS) // 5 seconds delay
                .addTag("test_worker")
                .build()

            // Schedule the daily notification work request
            val dailyNotificationRequest = OneTimeWorkRequestBuilder<DailySpendingNotificationService>()
                .setConstraints(constraints)
                .setInitialDelay(calculateDelayUntilNextNotification(context), java.util.concurrent.TimeUnit.MILLISECONDS)
                .addTag("daily_spending")
                .build()

            // Schedule the test work first
            workManager.enqueue(testRequest)
            android.util.Log.d("NotificationScheduler", "Enqueued test worker")

            // Schedule the work
            workManager.enqueueUniqueWork(
                "daily_spending_notification",
                ExistingWorkPolicy.REPLACE,
                dailyNotificationRequest
            )
            android.util.Log.d("NotificationScheduler", "Enqueued daily notification work")

            // Schedule recurring daily notifications at 8 PM
            val dailyWorkRequest = PeriodicWorkRequestBuilder<DailySpendingNotificationService>(
                1, java.util.concurrent.TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(calculateDelayUntilNextNotification(context), java.util.concurrent.TimeUnit.MILLISECONDS)
                .addTag("daily_spending_recurring")
                .build()

            workManager.enqueueUniquePeriodicWork(
                "daily_spending_recurring",
                ExistingPeriodicWorkPolicy.REPLACE,
                dailyWorkRequest
            )
        }

        fun cancelDailyNotifications(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork("daily_spending_notification")
            workManager.cancelUniqueWork("daily_spending_recurring")
        }

        private fun calculateDelayUntilNextNotification(context: Context): Long {
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance()

            // Get user's preferred time from preferences
            val prefs = context.getSharedPreferences("fintrack_prefs", Context.MODE_PRIVATE)
            val timeString = prefs.getString("notification_time", "20:00") ?: "20:00"

            try {
                val timeParts = timeString.split(":")
                val hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()

                // Set to user's preferred time today
                calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
                calendar.set(java.util.Calendar.MINUTE, minute)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)

                // If it's already past the preferred time today, schedule for tomorrow
                if (calendar.timeInMillis <= now) {
                    calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
                }
            } catch (e: Exception) {
                // Fallback to 8:00 PM if time format is invalid
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 20)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)

                if (calendar.timeInMillis <= now) {
                    calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
                }
            }

            return calendar.timeInMillis - now
        }
    }
}