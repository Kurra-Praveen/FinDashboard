package com.kpr.fintrack.services.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationScheduler {

    companion object {
        private const val DAILY_SPENDING_WORK_NAME = "daily_spending_recurring"

        fun scheduleDailyNotifications(context: Context) {
            val workManager = WorkManager.getInstance(context)

            // Create constraints for the notification worker
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false) // Depending on the requirement, can be set to true
                .setRequiresCharging(false)
                .build()

            // Calculate the initial delay to the next notification time
            val initialDelay = calculateDelayUntilNextNotification(context)

            // Define the periodic work request
            val dailyWorkRequest = PeriodicWorkRequestBuilder<DailySpendingNotificationService>(
                1, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag(DAILY_SPENDING_WORK_NAME)
                .build()

            // Enqueue the unique periodic work
            workManager.enqueueUniquePeriodicWork(
                DAILY_SPENDING_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                dailyWorkRequest
            )

            android.util.Log.d("NotificationScheduler", "Enqueued unique periodic work for daily notifications with initial delay: $initialDelay ms")
        }

        fun cancelDailyNotifications(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(DAILY_SPENDING_WORK_NAME)
            android.util.Log.d("NotificationScheduler", "Cancelled daily notifications work.")
        }

        private fun calculateDelayUntilNextNotification(context: Context): Long {
            val now = Calendar.getInstance()
            val calendar = Calendar.getInstance()

            // Get user's preferred time from SharedPreferences
            val prefs = context.getSharedPreferences("fintrack_prefs", Context.MODE_PRIVATE)
            val timeString = prefs.getString("notification_time", "20:00") ?: "20:00"

            try {
                val timeParts = timeString.split(":")
                val hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()

                // Set calendar to the user's preferred time for today
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                // If the scheduled time is in the past, schedule it for the next day
                if (calendar.before(now)) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
            } catch (e: Exception) {
                // Fallback to a default time (e.g., 8:00 PM) if parsing fails
                calendar.set(Calendar.HOUR_OF_DAY, 20)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                if (calendar.before(now)) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            // Return the difference in milliseconds between the future time and now
            return calendar.timeInMillis - now.timeInMillis
        }
    }
}
