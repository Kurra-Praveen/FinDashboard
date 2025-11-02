package com.kpr.fintrack.data.manager

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.kpr.fintrack.MainActivity
import com.kpr.fintrack.R
import com.kpr.fintrack.domain.manager.AppNotificationManager
import com.kpr.fintrack.domain.model.DailySpendingNotification
import com.kpr.fintrack.presentation.navigation.FINTRACK_URI
import com.kpr.fintrack.services.notification.NotificationScheduler
import com.kpr.fintrack.utils.extensions.formatCurrency
import com.kpr.fintrack.utils.notification.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.net.URLEncoder
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AppNotificationManagerImpl"

// Channel IDs
private const val CHANNEL_TRANSACTIONS = "transactions"
private const val CHANNEL_BUDGETS = "budgets"
private const val CHANNEL_SUMMARY = "summary"
private const val CHANNEL_SCANNING = "inbox_scanner_channel"
const val NOTIFICATION_ID = 1001

// Preference Keys
private const val PREF_NAME = "notification_prefs"
private const val KEY_DAILY_NOTIFICATIONS_ENABLED = "daily_notifications_enabled"
private const val KEY_NOTIFICATION_TIME = "notification_time"
private const val KEY_NOTIFICATION_FREQUENCY = "notification_frequency"
private const val KEY_INSIGHTS_ENABLED = "insights_enabled"
private const val KEY_COMPARISON_ENABLED = "comparison_enabled"
private const val KEY_BUDGET_ALERTS_ENABLED = "budget_alerts_enabled"

@Singleton
class AppNotificationManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppNotificationManager {

    private val notificationManager = NotificationManagerCompat.from(context)
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val _settingsFlow = MutableStateFlow(loadSettings())

    init {
        Log.d(TAG, "Initializing AppNotificationManagerImpl")
        registerNotificationChannels()

        prefs.registerOnSharedPreferenceChangeListener { _, _ ->
            Log.d(TAG, "SharedPreferences changed — updating settings flow")
            _settingsFlow.value = loadSettings()
        }
    }

    override fun registerNotificationChannels() {
        Log.d(TAG, "Registering notification channels")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_TRANSACTIONS, "Transactions", NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Alerts for new or failed transactions." },

                NotificationChannel(
                    CHANNEL_BUDGETS, "Budget Alerts", NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Notifications when you approach a budget limit." },

                NotificationChannel(
                    CHANNEL_SUMMARY, "Daily Summary", NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Your daily spending report." },

                NotificationChannel(
                    CHANNEL_SCANNING, "Scanning", NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Status of inbox scanning." })

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannels(channels)
            Log.d(TAG, "Notification channels created successfully")
        } else {
            Log.d(TAG, "Skipping channel creation (API < 26)")
        }
    }

    override fun showTransactionAddedNotification(
        transactionId: Long, merchantName: String, amount: String
    ) {
        Log.d(
            TAG,
            "Showing TransactionAddedNotification for ID=$transactionId, merchant=$merchantName, amount=$amount"
        )
        val deepLinkUri = "$FINTRACK_URI/transaction_detail/$transactionId".toUri()

        val intent =
            Intent(Intent.ACTION_VIEW, deepLinkUri).apply { `package` = context.packageName }
        val pendingIntent = PendingIntent.getActivity(
            context,
            transactionId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val action = NotificationCompat.Action.Builder(
            R.drawable.ic_notification, "Review", pendingIntent
        ).build()

        val notification = NotificationCompat.Builder(context, CHANNEL_TRANSACTIONS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("New Transaction: $merchantName").setContentText("Amount: $amount")
            .setPriority(NotificationCompat.PRIORITY_HIGH).setContentIntent(pendingIntent)
            .addAction(action).setAutoCancel(true).build()

        notifySafely(transactionId.toInt(), notification)
    }

    override fun showTransactionFailedNotification(originalText: String) {
        Log.d(TAG, "Showing TransactionFailedNotification: $originalText")
        val encodedText = URLEncoder.encode(originalText, "UTF-8")
        val deepLinkUri = "$FINTRACK_URI/add_transaction?originalText=$encodedText".toUri()

        val intent =
            Intent(Intent.ACTION_VIEW, deepLinkUri).apply { `package` = context.packageName }
        val pendingIntent = PendingIntent.getActivity(
            context,
            originalText.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TRANSACTIONS)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("Transaction Parse Failed")
            .setContentText("Tap to add this transaction manually.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(originalText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).setContentIntent(pendingIntent)
            .setAutoCancel(true).build()

        notifySafely(originalText.hashCode(), notification)
    }

    override fun showDailySummaryNotification(data: DailySpendingNotification) {
        Log.d(
            TAG,
            "Showing DailySummaryNotification for ${data.date} with ${data.transactionCount} transactions"
        )
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
        val formattedDate = data.date.format(dateFormatter)
        val title = "📊 Daily Spending Summary - $formattedDate"
        val content = buildNotificationContent(data)

        val notification = NotificationCompat.Builder(context, CHANNEL_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent).setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE).build()

        notifySafely(1, notification)
    }

    private fun buildNotificationContent(data: DailySpendingNotification): String {
        Log.d(TAG, "Building notification content for DailySpendingNotification")
        val content = StringBuilder()
        content.append("💰 Total Spent: ${data.totalSpent.formatCurrency()}\n")

        if (data.totalIncome > BigDecimal.ZERO) {
            content.append("💵 Total Income: ${data.totalIncome.formatCurrency()}\n")
            content.append("📈 Net: ${data.netAmount.formatCurrency()}\n")
        }

        content.append("🛒 Transactions: ${data.transactionCount}\n")

        data.topCategory?.let { content.append("🏷️ Top Category: $it\n") }
        data.topMerchant?.let { content.append("🏪 Top Merchant: $it\n") }

        if (data.insights.isNotEmpty()) {
            content.append("\n💡 Insights:\n")
            data.insights.forEach { insight -> content.append("• $insight\n") }
        }

        data.comparisonWithYesterday?.let {
            val change = if (it > BigDecimal.ZERO) "+" else ""
            content.append("\n📊 vs Yesterday: $change${it.formatCurrency()}\n")
        }

        return content.toString()
    }

    override fun showBudgetAlertNotification(
        categoryName: String, progressPercent: Int, amountSpent: String, amountTotal: String
    ) {
        Log.d(TAG, "Showing BudgetAlertNotification for $categoryName ($progressPercent%)")
        val title = "Budget Alert: $categoryName"
        val content = "You have spent $amountSpent of $amountTotal ($progressPercent%)."

        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGETS)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true).build()

        notifySafely(categoryName.hashCode(), notification)
    }

    override fun showScanCompleteNotification(importedCount: Int) {
        Log.d(TAG, "Showing ScanCompleteNotification for $importedCount transactions")
        val mainIntent = PendingIntent.getActivity(
            context,
            100,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SCANNING)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("Inbox Scan Complete")
            .setContentText("Found and imported $importedCount new transactions.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).setContentIntent(mainIntent)
            .setAutoCancel(true).build()

        notifySafely(NOTIFICATION_ID + 1, notification)
    }

    override fun showScanErrorNotification(error: String) {
        Log.e(TAG, "Showing ScanErrorNotification: $error")
        val notification = NotificationCompat.Builder(context, CHANNEL_SCANNING)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("Inbox Scan Failed")
            .setContentText("An error occurred: $error")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true).build()

        notifySafely(1002, notification)
    }

    private fun notifySafely(id: Int, notification: android.app.Notification) {
        Log.d(TAG, "Attempting to show notification with ID=$id")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                Log.w(
                    TAG, "Missing POST_NOTIFICATIONS permission — cannot show notification (ID=$id)"
                )
                return
            }
        }

        notificationManager.notify(id, notification)
        Log.d(TAG, "Notification displayed successfully (ID=$id)")
    }

    override fun showTestNotification() {
        Log.d(TAG, "Showing TestNotification")
        val notification = NotificationCompat.Builder(context, CHANNEL_TRANSACTIONS)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("Test Notification")
            .setContentText("This is a test notification from FinDashboard.")
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()

        notifySafely(-99, notification)
    }

    private fun loadSettings(): NotificationHelper.NotificationSettings {
        Log.d(TAG, "Loading notification settings from SharedPreferences")
        return NotificationHelper.NotificationSettings(
            dailyNotificationsEnabled = prefs.getBoolean(KEY_DAILY_NOTIFICATIONS_ENABLED, false),
            notificationTime = prefs.getString(KEY_NOTIFICATION_TIME, "20:00") ?: "20:00",
            notificationFrequency = prefs.getString(KEY_NOTIFICATION_FREQUENCY, "daily") ?: "daily",
            insightsEnabled = prefs.getBoolean(KEY_INSIGHTS_ENABLED, true),
            comparisonEnabled = prefs.getBoolean(KEY_COMPARISON_ENABLED, true),
            budgetAlertsEnabled = prefs.getBoolean(KEY_BUDGET_ALERTS_ENABLED, true)
        )
    }

    override fun getNotificationSettings(): Flow<NotificationHelper.NotificationSettings> =
        _settingsFlow.asStateFlow()

    override fun areNotificationsEnabled(): Boolean {
        val enabled = notificationManager.areNotificationsEnabled()
        Log.d(TAG, "areNotificationsEnabled: $enabled")
        return enabled
    }

    override suspend fun setDailySpendingNotificationEnabled(enabled: Boolean) {
        Log.d(TAG, "Setting daily spending notifications: $enabled")
        prefs.edit { putBoolean(KEY_DAILY_NOTIFICATIONS_ENABLED, enabled) }

        if (enabled) {
            NotificationScheduler.scheduleDailySpendingWorker(
                context, _settingsFlow.value.notificationTime
            )
            Log.d(TAG, "Daily spending worker scheduled")
        } else {
            NotificationScheduler.cancelDailySpendingWorker(context)
            Log.d(TAG, "Daily spending worker cancelled")
        }
    }

    override suspend fun setNotificationTimePreference(time: String) {
        Log.d(TAG, "Setting notification time preference: $time")
        prefs.edit { putString(KEY_NOTIFICATION_TIME, time) }

        if (_settingsFlow.value.dailyNotificationsEnabled) {
            NotificationScheduler.scheduleDailySpendingWorker(context, time)
            Log.d(TAG, "Rescheduled daily worker with new time: $time")
        }
    }

    override suspend fun setNotificationFrequencyPreference(frequency: String) {
        Log.d(TAG, "Setting notification frequency: $frequency")
        prefs.edit { putString(KEY_NOTIFICATION_FREQUENCY, frequency) }
    }

    override suspend fun setNotificationInsightsPreference(enabled: Boolean) {
        Log.d(TAG, "Setting insights preference: $enabled")
        prefs.edit { putBoolean(KEY_INSIGHTS_ENABLED, enabled) }
    }

    override suspend fun setNotificationComparisonPreference(enabled: Boolean) {
        Log.d(TAG, "Setting comparison preference: $enabled")
        prefs.edit { putBoolean(KEY_COMPARISON_ENABLED, enabled) }
    }

    override suspend fun setNotificationBudgetAlertsPreference(enabled: Boolean) {
        Log.d(TAG, "Setting budget alerts preference: $enabled")
        prefs.edit { putBoolean(KEY_BUDGET_ALERTS_ENABLED, enabled) }
    }
}
