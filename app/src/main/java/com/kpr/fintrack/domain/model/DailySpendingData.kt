package com.kpr.fintrack.domain.model

import androidx.compose.runtime.Immutable
import java.math.BigDecimal
import java.time.LocalDate

@Immutable
data class DailySpendingData(
    val date: LocalDate,
    val totalSpent: BigDecimal,
    val totalIncome: BigDecimal,
    val netAmount: BigDecimal, // income - spent
    val transactionCount: Int,
    val topCategory: String?,
    val topMerchant: String?,
    val averageTransactionAmount: BigDecimal,
    val largestTransaction: BigDecimal,
    val spendingByCategory: List<CategorySpendingData>
)

@Immutable
data class DailySpendingNotification(
    val date: LocalDate,
    val totalSpent: BigDecimal,
    val totalIncome: BigDecimal,
    val netAmount: BigDecimal,
    val transactionCount: Int,
    val topCategory: String?,
    val topMerchant: String?,
    val spendingByCategory: List<CategorySpendingData>,
    val comparisonWithYesterday: BigDecimal?, // positive = spent more, negative = spent less
    val comparisonWithLastWeek: BigDecimal?, // positive = spent more, negative = spent less
    val isSpendingHigh: Boolean, // based on user's average or budget
    val insights: List<String> // AI-generated insights about spending patterns
)
