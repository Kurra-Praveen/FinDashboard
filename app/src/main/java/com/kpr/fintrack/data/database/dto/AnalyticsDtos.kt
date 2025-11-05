package com.kpr.fintrack.data.database.dto

import java.math.BigDecimal

/**
 * A simple data class to hold the result of a
 * SUM() and GROUP BY query for category spending.
 */
data class CategorySpendingDto(
    val categoryId: Long,
    val categoryName: String?,
    val categoryIcon: String?,
    val color: String?,
    val totalAmount: BigDecimal,
    val transactionsCount: Int?
)