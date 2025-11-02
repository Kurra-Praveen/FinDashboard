package com.kpr.fintrack.domain.manager

import com.kpr.fintrack.domain.model.DailySpendingNotification
import com.kpr.fintrack.utils.notification.NotificationHelper
import kotlinx.coroutines.flow.Flow

/**
 * A centralized manager for creating and displaying ALL
 * app-generated notifications.
 */
interface AppNotificationManager {

    /**
     * Creates all necessary notification channels on app startup.
     */
    fun registerNotificationChannels()

    /**
     * Shows a notification that a new transaction was successfully parsed.
     * Includes a "Review" action to deep-link to the edit screen.
     */
    fun showTransactionAddedNotification(
        transactionId: Long,
        merchantName: String,
        amount: String
    )

    /**
     * Shows a notification that a transaction failed to parse.
     * Includes an "Add Manually" action to deep-link to the manual entry screen.
     */
    fun showTransactionFailedNotification(originalText: String)

    /**
     * Shows the daily spending summary.
     */
    fun showDailySummaryNotification(data: DailySpendingNotification)

    /**
     * Shows an alert for budget progress (e.g., 20%, 50%, 80%, 100%).
     */
    fun showBudgetAlertNotification(
        categoryName: String,
        progressPercent: Int,
        amountSpent: String,
        amountTotal: String
    )

    /**
     * Shows a simple notification for Inbox Scan status.
     * Can be used for start, progress, and completion.
     */
    fun showScanCompleteNotification(importedCount: Int)

    // (NEW) Specific one-shot notification for a scan error.
    fun showScanErrorNotification(error: String)

    // (NEW) Add method for test notification
    fun showTestNotification()

    fun getNotificationSettings(): Flow<NotificationHelper.NotificationSettings>

    fun areNotificationsEnabled(): Boolean

    suspend fun setDailySpendingNotificationEnabled(enabled: Boolean)

    suspend fun setNotificationTimePreference(time: String)

    suspend fun setNotificationFrequencyPreference(frequency: String)

    suspend fun setNotificationInsightsPreference(enabled: Boolean)

    suspend fun setNotificationComparisonPreference(enabled: Boolean)

    suspend fun setNotificationBudgetAlertsPreference(enabled: Boolean)
}