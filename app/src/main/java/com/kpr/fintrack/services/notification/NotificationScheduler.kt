package com.kpr.fintrack.services.notification

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationScheduler {

    companion object {

        // Define a single, constant tag for your work
        private const val DAILY_SPENDING_WORK_TAG = "daily_spending_recurring"

        fun scheduleDailyNotifications(context: Context) {
            val workManager = WorkManager.getInstance(context)
            Log.d("NotificationScheduler", "Scheduling daily notifications with WorkManager")

            // Create constraints for the notification
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            // We only need ONE PeriodicWorkRequest.
            // WorkManager will handle rerunning it every 1 day.
            val dailyWorkRequest = PeriodicWorkRequestBuilder<DailySpendingNotificationService>(
                1, java.util.concurrent.TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(calculateDelayUntilNextNotification(context), java.util.concurrent.TimeUnit.MILLISECONDS)
                .addTag(DAILY_SPENDING_WORK_TAG) // Use the constant tag
                .build()

            // Enqueue the work as unique.
            // This will CANCEL and REPLACE any existing work with the same tag.
            // This is exactly what you want when the user changes the time.
            workManager.enqueueUniquePeriodicWork(
                DAILY_SPENDING_WORK_TAG,
                ExistingPeriodicWorkPolicy.REPLACE, // REPLACE or CANCEL_AND_REENQUEUE are both good
                dailyWorkRequest
            )

            Log.d("NotificationScheduler", "Enqueued unique periodic work")
        }

        fun cancelDailyNotifications(context: Context) {
            android.util.Log.d("NotificationScheduler", "Cancelling daily notification work")
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(DAILY_SPENDING_WORK_TAG)
        }

        private fun calculateDelayUntilNextNotification(context: Context): Long {
            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance()

            // Get user's preferred time from preferences
            val prefs = context.getSharedPreferences("fintrack_prefs", Context.MODE_PRIVATE)
            val timeString = prefs.getString("notification_time", "20:00") ?: "20:00"

            android.util.Log.d("NotificationScheduler", "Calculating delay. Preferred time: $timeString")

            try {
                val timeParts = timeString.split(":")
                val hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()

                // Set to user's preferred time today
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                // If it's already past the preferred time today, schedule for tomorrow
                if (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    android.util.Log.d("NotificationScheduler", "Time is past today. Scheduling for tomorrow at $hour:$minute")
                } else {
                    android.util.Log.d("NotificationScheduler", "Scheduling for today at $hour:$minute")
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationScheduler", "Failed to parse time string. Defaulting to 20:00", e)
                // Fallback to 8:00 PM if time format is invalid
                calendar.set(Calendar.HOUR_OF_DAY, 20)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                if (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            val delay = calendar.timeInMillis - now
            android.util.Log.d("NotificationScheduler", "Calculated delay: ${TimeUnit.MILLISECONDS.toMinutes(delay)} minutes")
            return delay
        }
    }
}
