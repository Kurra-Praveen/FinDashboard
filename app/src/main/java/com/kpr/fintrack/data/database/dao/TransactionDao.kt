package com.kpr.fintrack.data.database.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.kpr.fintrack.data.database.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDateTime

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getPaginatedTransactions(): PagingSource<Int, TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getPaginatedTransactions(limit: Int, offset: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getPaginatedTransactionsByAccountId(accountId: Long): PagingSource<Int, TransactionEntity>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getTransactionsByAccountIdAndDateRange(
        accountId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE merchantName LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE (:categoryIds = '' OR categoryId IN (
            WITH RECURSIVE split(word, rest) AS (
                SELECT '', :categoryIds || ',' 
                UNION ALL
                SELECT substr(rest, 0, instr(rest, ','))-1,
                    substr(rest, instr(rest, ',') + 1)
                FROM split WHERE rest <> ''
            )
            SELECT CAST(trim(word) AS INTEGER) FROM split WHERE word <> ''
        ))
        AND (:startDate IS NULL OR date >= :startDate)
        AND (:endDate IS NULL OR date <= :endDate)
        AND (:minAmount IS NULL OR ROUND(CAST(amount AS DECIMAL), 2) >= ROUND(CAST(:minAmount AS DECIMAL), 2))
        AND (:maxAmount IS NULL OR ROUND(CAST(amount AS DECIMAL), 2) <= ROUND(CAST(:maxAmount AS DECIMAL), 2))
        AND (:isDebit IS NULL OR isDebit = :isDebit)
        AND (:searchQuery IS NULL OR merchantName LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%')
        ORDER BY date DESC
    """)
    fun getFilteredTransactions(
        categoryIds: String = "",
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null,
        minAmount: BigDecimal? = null,
        maxAmount: BigDecimal? = null,
        isDebit: Boolean? = null,
        searchQuery: String? = null
    ): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE isDebit = :isDebit AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalAmountByType(
        isDebit: Boolean,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): BigDecimal?

    @Query("SELECT * FROM transactions WHERE referenceId = :referenceId LIMIT 1")
    suspend fun getTransactionByReferenceId(referenceId: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

}
