package com.kpr.fintrack.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.kpr.fintrack.data.database.dto.CategoryBudgetDetailsDto
import com.kpr.fintrack.data.database.dto.TotalBudgetDetailsDto
import com.kpr.fintrack.data.database.entities.BudgetEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface BudgetDao {

    @Upsert
    suspend fun upsertBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("SELECT * FROM budget_table WHERE categoryId = :categoryId AND startDate = :startDate")
    suspend fun getBudgetForCategory(categoryId: Long, startDate: Long): BudgetEntity?

    @Query("SELECT * FROM budget_table WHERE categoryId IS NULL AND startDate = :startDate")
    suspend fun getTotalBudget(startDate: Long): BudgetEntity?

    /**
     * Gets the "Total Monthly Budget" (where categoryId is null) and joins it with the
     * sum of ALL debit transactions within the specified date range.
     * * NOTE: We pass LocalDateTime and let Room's TypeConverters handle conversion
     * for the BETWEEN clause.
     */
    @Query("""
        SELECT 
            b.id as budgetId,
            b.amount as budgetAmount,
            b.startDate as budgetStartDate,
            -- Use COALESCE to force SUM(NULL) to be '0.0', which BigDecimal can parse
            COALESCE(SUM(t.amount), '0.0') as spentAmount
        FROM budget_table AS b
        LEFT JOIN transactions AS t ON t.isDebit = 1 
                                     AND t.date BETWEEN :startDate AND :endDate
        WHERE b.categoryId IS NULL AND b.startDate = :startOfMonthTimestamp
        GROUP BY b.id, b.amount, b.startDate -- Group by all non-aggregated columns
    """)
    fun getTotalBudgetWithProgress(
        startOfMonthTimestamp: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<TotalBudgetDetailsDto?>

    /**
     * Gets all category-specific budgets, joins them with their category info,
     * and calculates the sum of debit transactions for each category within the date range.
     */
    @Query("""
        SELECT 
            b.id as budgetId,
            b.amount as budgetAmount,
            b.categoryId as categoryId,
            b.startDate as budgetStartDate,
            c.name as categoryName, 
            c.icon as categoryIcon, 
            c.color as categoryColor,
            -- Use COALESCE to force SUM(NULL) to be '0.0'
            COALESCE(SUM(t.amount), '0.0') as spentAmount
        FROM budget_table AS b
        JOIN categories AS c ON b.categoryId = c.id
        LEFT JOIN transactions AS t ON b.categoryId = t.categoryId 
                                     AND t.isDebit = 1
                                     AND t.date BETWEEN :startDate AND :endDate
        WHERE b.categoryId IS NOT NULL
        -- Group by all non-aggregated columns
        GROUP BY b.id, b.amount, b.categoryId, b.startDate, c.name, c.icon, c.color
    """)
    fun getCategoryBudgetsWithProgress(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<CategoryBudgetDetailsDto>>
}