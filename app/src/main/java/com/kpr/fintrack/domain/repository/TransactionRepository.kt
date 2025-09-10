package com.kpr.fintrack.domain.repository

import com.kpr.fintrack.domain.model.Transaction
import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.model.UpiApp
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDateTime

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<Transaction>>
    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>>
    fun searchTransactions(query: String): Flow<List<Transaction>>
    fun getFilteredTransactions(filter: TransactionFilter): Flow<List<Transaction>>
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

    // UPI App operations
    fun getAllUpiApps(): Flow<List<UpiApp>>
    suspend fun getUpiAppById(id: Long): UpiApp?
}

data class TransactionFilter(
    val categoryIds: List<Long>? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val minAmount: BigDecimal? = null,
    val maxAmount: BigDecimal? = null,
    val isDebit: Boolean? = null,
    val searchQuery: String? = null
)
