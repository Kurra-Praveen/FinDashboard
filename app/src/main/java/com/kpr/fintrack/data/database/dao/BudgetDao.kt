package com.kpr.fintrack.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.kpr.fintrack.data.database.dto.CategoryBudgetDetailsDto
import com.kpr.fintrack.data.database.dto.TotalBudgetDetailsDto
import com.kpr.fintrack.data.database.entities.BudgetEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDateTime

@Dao
interface BudgetDao {

    @Upsert
    suspend fun upsertBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("SELECT * FROM budget_table WHERE categoryId = :categoryId AND startDate = :startDate")
    suspend fun getBudgetForCategory(categoryId: Long, startDate: Long): BudgetEntity?


    @Query("""
        SELECT 
            b.id as budgetId,
            b.amount as budgetAmount,
            b.categoryId as categoryId,
            b.startDate as budgetStartDate,
            c.name as categoryName, 
            c.icon as categoryIcon, 
            c.color as categoryColor,
            COALESCE(SUM(t.amount), '0.0') as spentAmount
        FROM budget_table AS b
        JOIN categories AS c ON b.categoryId = c.id
        LEFT JOIN transactions AS t ON b.categoryId = t.categoryId 
                                     AND t.isDebit = 1
                                     AND t.date BETWEEN :startDate AND :endDate
        WHERE b.categoryId IS NOT NULL
        GROUP BY b.id, b.amount, b.categoryId, b.startDate, c.name, c.icon, c.color
    """)
    fun getCategoryBudgetsWithProgress(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<CategoryBudgetDetailsDto>>

    @Query("""
        SELECT COALESCE(SUM(amount), '0.0') 
        FROM budget_table
        WHERE categoryId IS NOT NULL 
        AND startDate = :startOfMonthTimestamp
    """)
    fun getTotalBudgetLimit(startOfMonthTimestamp: Long): Flow<BigDecimal>
}