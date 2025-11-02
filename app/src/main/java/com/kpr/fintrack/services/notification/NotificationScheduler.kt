package com.kpr.fintrack.services.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationScheduler {

    companion object {

        // Define a single, constant tag for your work
        private const val DAILY_SPENDING_WORK_TAG = "daily_spending_recurring"
        private const val DAILY_SUMMARY_WORKER_TAG = "DailySummaryWorker"
        fun scheduleDailySpendingWorker(context: Context, time: String) {
            val workManager = WorkManager.getInstance(context)

            // Cancel any old job
            cancelDailySpendingWorker(context)

            // Calculate delay until next notification
            val (hour, minute) = time.split(":").map { it.toInt() }
            val now = Calendar.getInstance()
            val nextRun = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }

            if (nextRun.before(now)) {
                nextRun.add(Calendar.DAY_OF_YEAR, 1)
            }

            val initialDelay = nextRun.timeInMillis - now.timeInMillis

            val workRequest = PeriodicWorkRequestBuilder<DailySpendingNotificationService>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                DAILY_SUMMARY_WORKER_TAG,
                ExistingPeriodicWorkPolicy.KEEP, // KEEP old one if it's already scheduled
                workRequest
            )
        }
        fun cancelDailySpendingWorker(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(DAILY_SUMMARY_WORKER_TAG)
        }
      }
}
