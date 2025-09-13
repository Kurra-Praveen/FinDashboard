package com.kpr.fintrack.domain.model

import androidx.compose.runtime.Immutable
import java.math.BigDecimal
import java.time.YearMonth

@Immutable
data class MonthlySpendingData(
    val yearMonth: YearMonth,
    val monthName: String, // "Sep 2025"
    val totalSpent: BigDecimal,
    val totalIncome: BigDecimal,
    val netAmount: BigDecimal // income - spent
)

@Immutable
data class CategorySpendingData(
    val categoryName: String,
    val categoryIcon: String,
    val amount: BigDecimal,
    val percentage: Float,
    val transactionCount: Int
)

@Immutable
data class WeeklySpendingData(
    val weekNumber: Int,
    val weekRange: String, // "Sep 1-7"
    val amount: BigDecimal
)

@Immutable
data class TopMerchantData(
    val merchantName: String,
    val amount: BigDecimal,
    val transactionCount: Int
)

@Immutable
data class AnalyticsSummary(
    val monthlyData: List<MonthlySpendingData>,
    val categoryData: List<CategorySpendingData>,
    val weeklyData: List<WeeklySpendingData>,
    val topMerchants: List<TopMerchantData>,
    val averageDailySpending: BigDecimal,
    val highestSpendingDay: BigDecimal,
    val mostUsedCategory: String
)
