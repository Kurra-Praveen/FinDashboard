package com.kpr.fintrack.domain.model

import java.math.BigDecimal
import java.time.YearMonth

/**
 * Represents a basic budget set by the user.
 */
data class Budget(
    val id: Long,
    val amount: BigDecimal,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val categoryId: Long?, // Null for "Total Budget"
    val yearMonth: YearMonth // Represents the month this budget is for
)

enum class BudgetPeriod {
    MONTHLY
    // We can add WEEKLY, YEARLY here in the future
}

/**
 * A rich model for UI display.
 * This combines a Budget with its real-time progress.
 */
data class BudgetDetails(
    val budget: Budget,
    val spent: BigDecimal,
    val categoryName: String? = null, // Null for "Total Budget"
    val categoryIcon: String? = null, // Null for "Total Budget"
    val categoryColor: String? = null // Null for "Total Budget"
) {
    val remaining: BigDecimal = budget.amount - spent
    val progress: Float = if (budget.amount > BigDecimal.ZERO) {
        (spent / budget.amount).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val isOverspent: Boolean = spent > budget.amount
}