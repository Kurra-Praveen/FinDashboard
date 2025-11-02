package com.kpr.fintrack.services.notification

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kpr.fintrack.domain.manager.AppNotificationManager
import com.kpr.fintrack.domain.model.DailySpendingNotification
import com.kpr.fintrack.domain.repository.TransactionRepository
import com.kpr.fintrack.utils.extensions.formatCurrency
import com.kpr.fintrack.utils.logging.SecureLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.LocalDate

@HiltWorker
class DailySpendingNotificationService @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val secureLogger: SecureLogger,
    private val appNotificationManager: AppNotificationManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID = "daily_spending_channel"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_NAME = "Daily Spending Analytics"
        private const val CHANNEL_DESCRIPTION = "Daily spending summary and analytics notifications"
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        return try {
            secureLogger.d("DailySpendingNotification", "Starting daily spending notification work")

            // Check if notifications are enabled
            if (!isNotificationEnabled()) {
                secureLogger.d("DailySpendingNotification", "Notifications disabled, skipping")
                return Result.success()
            }

            // Get today's spending data
            val todayData = getTodaySpendingData()

            if (todayData == null) {
                secureLogger.d("DailySpendingNotification", "No spending data for today")
                return Result.success()
            }

            // (MODIFIED) Create and show notification via the manager
            appNotificationManager.showDailySummaryNotification(todayData)

            secureLogger.i(
                "DailySpendingNotification", "Daily spending notification sent successfully"
            )
            Result.success()

        } catch (e: Exception) {
            secureLogger.e(
                "DailySpendingNotification", "Failed to send daily spending notification", e
            )
            Result.failure()
        }
    }

    private suspend fun getTodaySpendingData(): DailySpendingNotification? {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay()
        val endOfDay = today.atTime(23, 59, 59)

        // Get today's transactions
        val todayTransactions =
            transactionRepository.getTransactionsByDateRange(startOfDay, endOfDay).first()

        if (todayTransactions.isEmpty()) {
            return null
        }

        // Calculate spending metrics
        val debitTransactions = todayTransactions.filter { it.isDebit }
        val creditTransactions = todayTransactions.filter { !it.isDebit }

        val totalSpent = debitTransactions.sumOf { it.amount }
        val totalIncome = creditTransactions.sumOf { it.amount }
        val netAmount = totalIncome - totalSpent
        val transactionCount = todayTransactions.size

        // Get top category and merchant
        val topCategory = debitTransactions.groupBy { it.category.name }
            .maxByOrNull { it.value.sumOf { tx -> tx.amount } }?.key

        val topMerchant = debitTransactions.groupBy { it.merchantName }
            .maxByOrNull { it.value.sumOf { tx -> tx.amount } }?.key

        // Calculate average and largest transaction
        val averageTransactionAmount = if (debitTransactions.isNotEmpty()) {
            totalSpent / BigDecimal(debitTransactions.size)
        } else BigDecimal.ZERO

        val largestTransaction = debitTransactions.maxOfOrNull { it.amount } ?: BigDecimal.ZERO

        // Get spending by category
        val spendingByCategory =
            debitTransactions.groupBy { it.category }.map { (category, transactions) ->
                    val amount = transactions.sumOf { it.amount }
                    val percentage = if (totalSpent > BigDecimal.ZERO) {
                        (amount / totalSpent * BigDecimal(100)).toFloat()
                    } else 0f

                    com.kpr.fintrack.domain.model.CategorySpendingData(
                        categoryName = category.name,
                        categoryIcon = category.icon,
                        amount = amount,
                        percentage = percentage,
                        transactionCount = transactions.size,
                        color = category.color
                    )
                }.sortedByDescending { it.amount }

        // Get comparison data - FIXED: Pass totalSpent directly to avoid recursion
        val yesterdayComparison = getYesterdayComparison(today, totalSpent)
        val lastWeekComparison = getLastWeekComparison(today, totalSpent)

        // Generate insights
        val insights = generateInsights(
            totalSpent = totalSpent,
            totalIncome = totalIncome,
            transactionCount = transactionCount,
            topCategory = topCategory,
            topMerchant = topMerchant,
            yesterdayComparison = yesterdayComparison,
            lastWeekComparison = lastWeekComparison
        )

        return DailySpendingNotification(
            date = today,
            totalSpent = totalSpent,
            totalIncome = totalIncome,
            netAmount = netAmount,
            transactionCount = transactionCount,
            topCategory = topCategory,
            topMerchant = topMerchant,
            spendingByCategory = spendingByCategory,
            comparisonWithYesterday = yesterdayComparison,
            comparisonWithLastWeek = lastWeekComparison,
            isSpendingHigh = isSpendingHigh(totalSpent),
            insights = insights
        )
    }

    // FIXED: Accept todaySpent as parameter to avoid recursion
    private suspend fun getYesterdayComparison(
        today: LocalDate, todaySpent: BigDecimal
    ): BigDecimal? {
        val yesterday = today.minusDays(1)
        val startOfYesterday = yesterday.atStartOfDay()
        val endOfYesterday = yesterday.atTime(23, 59, 59)

        val yesterdayTransactions =
            transactionRepository.getTransactionsByDateRange(startOfYesterday, endOfYesterday)
                .first()

        val yesterdaySpent = yesterdayTransactions.filter { it.isDebit }.sumOf { it.amount }

        return if (yesterdaySpent > BigDecimal.ZERO) {
            todaySpent - yesterdaySpent
        } else null
    }

    // FIXED: Accept todaySpent as parameter to avoid recursion
    private suspend fun getLastWeekComparison(
        today: LocalDate, todaySpent: BigDecimal
    ): BigDecimal? {
        val lastWeekSameDay = today.minusWeeks(1)
        val startOfLastWeek = lastWeekSameDay.atStartOfDay()
        val endOfLastWeek = lastWeekSameDay.atTime(23, 59, 59)

        val lastWeekTransactions =
            transactionRepository.getTransactionsByDateRange(startOfLastWeek, endOfLastWeek).first()

        val lastWeekSpent = lastWeekTransactions.filter { it.isDebit }.sumOf { it.amount }

        return if (lastWeekSpent > BigDecimal.ZERO) {
            todaySpent - lastWeekSpent
        } else null
    }

    private fun isSpendingHigh(totalSpent: BigDecimal): Boolean {
        // Simple heuristic: consider spending high if > ₹1000
        // In a real app, this would be based on user's budget or historical average
        return totalSpent > BigDecimal(1000)
    }

    private fun generateInsights(
        totalSpent: BigDecimal,
        totalIncome: BigDecimal,
        transactionCount: Int,
        topCategory: String?,
        topMerchant: String?,
        yesterdayComparison: BigDecimal?,
        lastWeekComparison: BigDecimal?
    ): List<String> {
        val insights = mutableListOf<String>()

        // Spending level insights
        when {
            totalSpent == BigDecimal.ZERO -> insights.add("No spending today! Great job saving money.")
            totalSpent < BigDecimal(100) -> insights.add("Low spending day - you're being frugal!")
            totalSpent > BigDecimal(2000) -> insights.add("High spending day - consider reviewing your expenses.")
        }

        // Income vs spending
        if (totalIncome > BigDecimal.ZERO) {
            val savingsRate = ((totalIncome - totalSpent) / totalIncome * BigDecimal(100)).toFloat()
            when {
                savingsRate > 50f -> insights.add("Excellent savings rate of ${savingsRate.toInt()}%!")
                savingsRate > 20f -> insights.add("Good savings rate of ${savingsRate.toInt()}%")
                savingsRate < 0f -> insights.add("Spent more than earned today - consider budgeting")
            }
        }

        // Transaction count insights
        when {
            transactionCount > 10 -> insights.add("Busy day with $transactionCount transactions")
            transactionCount == 1 -> insights.add("Single transaction day - very focused spending")
        }

        // Category insights
        topCategory?.let { category ->
            insights.add("Most spent on: $category")
        }

        // Comparison insights
        yesterdayComparison?.let { comparison ->
            when {
                comparison > BigDecimal.ZERO -> insights.add("Spent ₹${comparison.formatCurrency()} more than yesterday")
                comparison < BigDecimal.ZERO -> insights.add("Spent ₹${(-comparison).formatCurrency()} less than yesterday")
            }
        }

        lastWeekComparison?.let { comparison ->
            when {
                comparison > BigDecimal.ZERO -> insights.add("Spent ₹${comparison.formatCurrency()} more than last week")
                comparison < BigDecimal.ZERO -> insights.add("Spent ₹${(-comparison).formatCurrency()} less than last week")
            }
        }

        return insights.take(3) // Limit to 3 insights
    }
    private fun isNotificationEnabled(): Boolean {
        val prefs = applicationContext.getSharedPreferences("fintrack_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("daily_notifications_enabled", true)
    }
}