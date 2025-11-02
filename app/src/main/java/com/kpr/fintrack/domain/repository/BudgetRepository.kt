package com.kpr.fintrack.domain.repository

import com.kpr.fintrack.domain.model.Budget
import com.kpr.fintrack.domain.model.BudgetDetails
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Repository for managing Budgets.
 * This provides a clean API for the ViewModels to use.
 */
interface BudgetRepository {

    /**
     * Gets the "Total Monthly Budget" and its progress for a given month.
     * Emits null if no total budget is set.
     */
    fun getTotalBudgetDetails(month: YearMonth): Flow<BudgetDetails?>

    /**
     * Gets all category-specific budgets and their progress for a given month.
     * Emits an empty list if no category budgets are set.
     */
    fun getCategoryBudgetDetails(month: YearMonth): Flow<List<BudgetDetails>>

    /**
     * Saves or updates a budget (either total or category-specific).
     */
    suspend fun saveBudget(amount: BigDecimal, categoryId: Long?, month: YearMonth)

    /**
     * Deletes a budget.
     */
    suspend fun deleteBudget(budgetId: Long)

    /**
     * Gets the raw budget data for a specific category for a given month.
     * Used by the UI to know if a budget is already set.
     */
    suspend fun getBudgetForCategory(categoryId: Long, month: YearMonth): Budget?

    /**
     * Gets the raw total budget data for a given month.
     */
    suspend fun getTotalBudget(month: YearMonth): Budget?

}