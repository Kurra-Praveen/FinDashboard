package com.kpr.fintrack.data.database.dto

import java.math.BigDecimal

/**
 * DTO for fetching category-specific budgets with their progress.
 * FLATTENED to avoid @Embedded issues.
 */
data class CategoryBudgetDetailsDto(
    // Budget fields
    val budgetId: Long,
    val budgetAmount: BigDecimal,
    val categoryId: Long,
    val budgetStartDate: Long,

    // Category fields
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,

    // Calculated field
    val spentAmount: BigDecimal? // Nullable to safely accept SUM(NULL)
)

/**
 * DTO for fetching the "Total Budget" progress.
 * FLATTENED to avoid @Embedded issues.
 */
data class TotalBudgetDetailsDto(
    // Budget fields
    val budgetId: Long,
    val budgetAmount: BigDecimal,
    val budgetStartDate: Long,

    // Calculated field
    val spentAmount: BigDecimal? // Nullable to safely accept SUM(NULL)
)