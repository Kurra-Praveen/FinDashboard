package com.kpr.fintrack.domain.repository

import androidx.paging.PagingData
import com.kpr.fintrack.domain.model.AnalyticsSummary
import com.kpr.fintrack.domain.model.Transaction
import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.model.CategorySpendingData
import com.kpr.fintrack.domain.model.MonthlySpendingData
import com.kpr.fintrack.domain.model.TopMerchantData
import com.kpr.fintrack.domain.model.UpiApp
import com.kpr.fintrack.domain.model.WeeklySpendingData
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDateTime

interface TransactionRepository {
    fun getPaginatedTransactions(): Flow<PagingData<Transaction>>
    fun getPaginatedTransactionsByAccountId(accountId: Long): Flow<PagingData<Transaction>>
    fun getTransactionsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<Transaction>>
    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>>
    fun searchTransactions(query: String): Flow<List<Transaction>>
    fun getFilteredTransactions(filter: TransactionFilter): Flow<PagingData<Transaction>>
    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>>

    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun insertTransactions(transactions: List<Transaction>)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun getTransactionByReferenceId(referenceId: String): Transaction?
    suspend fun getTotalSpending(startDate: LocalDateTime, endDate: LocalDateTime): BigDecimal
    suspend fun getTotalCredits(startDate: LocalDateTime, endDate: LocalDateTime): BigDecimal
    suspend fun getTransactionCount(): Int

    // Category operations
    fun getAllCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: Long): Category?
    suspend fun insertCategory(category: Category): Long
    suspend fun getCategoriesByKeyword(keyword: String): List<Category>

    suspend fun updateCategory(category: Category)

    suspend fun deleteCategory(category: Category)

    // UPI App operations
    fun getAllUpiApps(): Flow<List<UpiApp>>
    suspend fun getUpiAppById(id: Long): UpiApp?

    // Add to TransactionRepository interface
    suspend fun getTransactionById(id: Long): Transaction?

    suspend fun getMonthlySpendingData(monthsBack: Int = 6): List<MonthlySpendingData>
    suspend fun getCategorySpendingData(startDate: LocalDateTime, endDate: LocalDateTime): List<CategorySpendingData>
    suspend fun getWeeklySpendingData(weeksBack: Int = 4): List<WeeklySpendingData>
    suspend fun getTopMerchants(limit: Int = 10, startDate: LocalDateTime, endDate: LocalDateTime): List<TopMerchantData>
    suspend fun getAnalyticsSummary(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): AnalyticsSummary

    fun getTotalBudgetedSpending(startDate: LocalDateTime, endDate: LocalDateTime, startOfMonthTimestamp: Long): Flow<BigDecimal>
}

data class TransactionFilter(
    val categoryIds: List<Long>? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val minAmount: BigDecimal? = null,
    val maxAmount: BigDecimal? = null,
    val isDebit: Boolean? = null,
    val searchQuery: String? = null,
    val sortOrder: String? = null
)
