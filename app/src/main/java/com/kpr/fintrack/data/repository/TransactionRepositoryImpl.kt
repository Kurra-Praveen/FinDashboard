package com.kpr.fintrack.data.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.kpr.fintrack.data.database.dao.AccountDao
import com.kpr.fintrack.data.database.dao.CategoryDao
import com.kpr.fintrack.data.database.dao.TransactionDao
import com.kpr.fintrack.data.database.dao.UpiAppDao
import com.kpr.fintrack.data.mapper.toDomainModel
import com.kpr.fintrack.data.mapper.toEntity
import com.kpr.fintrack.domain.model.AnalyticsSummary
import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.model.CategorySpendingData
import com.kpr.fintrack.domain.model.MonthlySpendingData
import com.kpr.fintrack.domain.model.TopMerchantData
import com.kpr.fintrack.domain.model.Transaction
import com.kpr.fintrack.domain.model.UpiApp
import com.kpr.fintrack.domain.model.WeeklySpendingData
import com.kpr.fintrack.domain.repository.TransactionFilter
import com.kpr.fintrack.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val upiAppDao: UpiAppDao,
    private val accountDao: AccountDao
) : TransactionRepository {
    companion object {
        private const val TAG = "TransactionRepoImpl"
    }

    init {
        Log.d(TAG, "Repository initialized")
    }

    override fun getPaginatedTransactions(): Flow<PagingData<Transaction>> {
        return try {
            Pager(
                config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                pagingSourceFactory = { transactionDao.getPaginatedTransactions() }
            ).flow.map { pagingData ->
                pagingData.map { it.toDomainModel(accountDao) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paginated transactions: ${e.message}", e)
            throw e
        }
    }

    override fun getPaginatedTransactionsByAccountId(accountId: Long): Flow<PagingData<Transaction>> {
        Log.d(TAG, "Getting paginated transactions for accountId=$accountId")
        return try {
            Pager(
                config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                pagingSourceFactory = { transactionDao.getPaginatedTransactionsByAccountId(accountId) }
            ).flow.map { pagingData ->
                pagingData.map { it.toDomainModel(accountDao) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paginated transactions for accountId=$accountId: ${e.message}", e)
            throw e
        }
    }

    override fun getTransactionsByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<Transaction>> {
        Log.d(TAG, "Getting transactions between $startDate and $endDate")
        return try {
            transactionDao.getTransactionsByDateRange(startDate, endDate).map { entities ->
                entities.asFlow().map { it.toDomainModel(accountDao) }.toList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting transactions by date range: ${e.message}", e)
            throw e
        }
    }

    override fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(categoryId).map { entities ->
            entities.asFlow().map { it.toDomainModel(accountDao) }.toList()
        }
    }

    override fun searchTransactions(query: String): Flow<List<Transaction>> {
        return transactionDao.searchTransactions(query).map { entities ->
            entities.asFlow().map { it.toDomainModel(accountDao) }.toList()
        }
    }

    override fun getFilteredTransactions(filter: TransactionFilter): Flow<PagingData<Transaction>> {
        android.util.Log.d("TransactionRepositoryImpl", "getFilteredTransactions called with filter: $filter")
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                transactionDao.getFilteredTransactions(
                    categoryIds = filter.categoryIds?.joinToString(",") ?: "",
                    startDate = filter.startDate,
                    endDate = filter.endDate,
                    minAmount = filter.minAmount,
                    maxAmount = filter.maxAmount,
                    isDebit = filter.isDebit,
                    searchQuery = filter.searchQuery,
                    sortOrder = filter.sortOrder ?: "desc"
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel(accountDao) }
        }
    }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> {
        return try {
            transactionDao.getRecentTransactions(limit).map { entities ->
                entities.asFlow().map { it.toDomainModel(accountDao) }.toList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recent transactions with limit=$limit: ${e.message}", e)
            throw e
        }
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        return try {
            val result = transactionDao.insertTransaction(transaction.toEntity())
            Log.d(TAG, "Successfully inserted transaction with id=$result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting transaction: ${e.message}", e)
            throw e
        }
    }

    override suspend fun insertTransactions(transactions: List<Transaction>) {
        try {
            transactionDao.insertTransactions(transactions.map { it.toEntity() })
            Log.d(TAG, "Successfully inserted ${transactions.size} transactions")
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting multiple transactions: ${e.message}", e)
            throw e
        }
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        try {
            transactionDao.updateTransaction(transaction.toEntity())
            Log.d(TAG, "Successfully updated transaction with id=${transaction.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating transaction ${transaction.id}: ${e.message}", e)
            throw e
        }
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        try {
            transactionDao.deleteTransaction(transaction.toEntity())
            Log.d(TAG, "Successfully deleted transaction with id=${transaction.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting transaction ${transaction.id}: ${e.message}", e)
            throw e
        }
    }

    override suspend fun getTransactionByReferenceId(referenceId: String): Transaction? {
        return transactionDao.getTransactionByReferenceId(referenceId)?.toDomainModel(accountDao)
    }

    override suspend fun getTotalSpending(startDate: LocalDateTime, endDate: LocalDateTime): BigDecimal {
        return transactionDao.getTotalAmountByType(true, startDate, endDate) ?: BigDecimal.ZERO
    }

    override suspend fun getTotalCredits(startDate: LocalDateTime, endDate: LocalDateTime): BigDecimal {
        return transactionDao.getTotalAmountByType(false, startDate, endDate) ?: BigDecimal.ZERO
    }

    override suspend fun getTransactionCount(): Int {
        return transactionDao.getTransactionCount()
    }

    // Category operations
    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)?.toDomainModel()
    }

    override suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun getCategoriesByKeyword(keyword: String): List<Category> {
        return categoryDao.getCategoriesByKeyword(keyword).map { it.toDomainModel() }
    }

    // UPI App operations
    override fun getAllUpiApps(): Flow<List<UpiApp>> {
        return upiAppDao.getAllUpiApps().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getUpiAppById(id: Long): UpiApp? {
        return upiAppDao.getUpiAppById(id)?.toDomainModel()
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomainModel(accountDao)
    }

    // Add to TransactionRepositoryImpl class
    override suspend fun getMonthlySpendingData(monthsBack: Int): List<MonthlySpendingData> {
        val currentMonth = YearMonth.now()
        val monthlyData = mutableListOf<MonthlySpendingData>()

        for (i in monthsBack downTo 0) {
            val targetMonth = currentMonth.minusMonths(i.toLong())
            val startDate = targetMonth.atDay(1).atStartOfDay()
            val endDate = targetMonth.atEndOfMonth().atTime(23, 59, 59)

            val transactions = transactionDao.getTransactionsByDateRange(startDate, endDate).first()

            val totalSpent = transactions.filter { it.isDebit }.sumOf { it.amount }
            val totalIncome = transactions.filter { !it.isDebit }.sumOf { it.amount }

            monthlyData.add(
                MonthlySpendingData(
                    yearMonth = targetMonth,
                    monthName = targetMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())),
                    totalSpent = totalSpent,
                    totalIncome = totalIncome,
                    netAmount = totalIncome - totalSpent
                )
            )
        }

        return monthlyData
    }

    override suspend fun getCategorySpendingData(startDate: LocalDateTime, endDate: LocalDateTime): List<CategorySpendingData> {
        val transactions = transactionDao.getTransactionsByDateRange(startDate, endDate).first()
        val debitTransactions = transactions.filter { it.isDebit }
        val totalSpent = debitTransactions.sumOf { it.amount }

        return debitTransactions
            .groupBy { it.categoryId }
            .map { (category, categoryTransactions) ->
                val categoryAmount = categoryTransactions.sumOf { it.amount }
                val percentage = if (totalSpent > BigDecimal.ZERO) {
                    (categoryAmount / totalSpent * BigDecimal(100)).toFloat()
                } else 0f

                CategorySpendingData(
                    categoryName = Category.getDefaultCategories().find { x -> x.id==category}?.name ?: "Unknown",
                    categoryIcon = Category.getDefaultCategories().find { x -> x.id==category}?.icon ?: "Unknown",
                    amount = categoryAmount,
                    percentage = percentage,
                    transactionCount = categoryTransactions.size
                )
            }
            .sortedByDescending { it.amount }
    }

    override suspend fun getWeeklySpendingData(weeksBack: Int): List<WeeklySpendingData> {
        val currentWeek = LocalDate.now()
        val weeklyData = mutableListOf<WeeklySpendingData>()

        for (i in weeksBack downTo 0) {
            val weekStart = currentWeek.minusWeeks(i.toLong()).with(DayOfWeek.MONDAY)
            val weekEnd = weekStart.with(DayOfWeek.SUNDAY)

            val startDateTime = weekStart.atStartOfDay()
            val endDateTime = weekEnd.atTime(23, 59, 59)

            val weekTransactions = transactionDao.getTransactionsByDateRange(startDateTime, endDateTime).first()
            val weekSpending = weekTransactions.filter { it.isDebit }.sumOf { it.amount }

            weeklyData.add(
                WeeklySpendingData(
                    weekNumber = i + 1,
                    weekRange = "${weekStart.format(DateTimeFormatter.ofPattern("MMM d"))}-${weekEnd.dayOfMonth}",
                    amount = weekSpending
                )
            )
        }

        return weeklyData.reversed()
    }

    override suspend fun getTopMerchants(limit: Int, startDate: LocalDateTime, endDate: LocalDateTime): List<TopMerchantData> {
        val transactions = transactionDao.getTransactionsByDateRange(startDate, endDate).first()

        return transactions
            .filter { it.isDebit }
            .groupBy { it.merchantName }
            .map { (merchant, merchantTransactions) ->
                TopMerchantData(
                    merchantName = merchant,
                    amount = merchantTransactions.sumOf { it.amount },
                    transactionCount = merchantTransactions.size
                )
            }
            .sortedByDescending { it.amount }
            .take(limit)
    }

    override suspend fun getAnalyticsSummary(): AnalyticsSummary {
        Log.d(TAG, "Generating analytics summary")
        return try {
            val currentMonth = YearMonth.now()
            val startOfMonth = currentMonth.atDay(1).atStartOfDay()
            val endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59)

            AnalyticsSummary(
                monthlyData = getMonthlySpendingData(6),
                categoryData = getCategorySpendingData(startOfMonth, endOfMonth),
                weeklyData = getWeeklySpendingData(4),
                topMerchants = getTopMerchants(5, startOfMonth, endOfMonth),
                averageDailySpending = calculateAverageDailySpending(startOfMonth, endOfMonth),
                highestSpendingDay = calculateHighestSpendingDay(startOfMonth, endOfMonth),
                mostUsedCategory = findMostUsedCategory(startOfMonth, endOfMonth)
            ).also {
                Log.d(TAG, "Successfully generated analytics summary")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating analytics summary: ${e.message}", e)
            throw e
        }
    }

    private suspend fun calculateAverageDailySpending(startDate: LocalDateTime, endDate: LocalDateTime): BigDecimal {
        return try {
            val transactions = transactionDao.getTransactionsByDateRange(startDate, endDate).first()
            val totalSpent = transactions.filter { it.isDebit }.sumOf { it.amount }
            val daysDiff = ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate()) + 1

            if (daysDiff > 0) totalSpent / BigDecimal(daysDiff) else BigDecimal.ZERO
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating average daily spending: ${e.message}", e)
            throw e
        }
    }

    private suspend fun calculateHighestSpendingDay(startDate: LocalDateTime, endDate: LocalDateTime): BigDecimal {
        val transactions = transactionDao.getTransactionsByDateRange(startDate, endDate).first()

        return transactions
            .filter { it.isDebit }
            .groupBy { it.date.toLocalDate() }
            .maxByOrNull { it.value.sumOf { tx -> tx.amount } }
            ?.value?.sumOf { it.amount } ?: BigDecimal.ZERO
    }

    private suspend fun findMostUsedCategory(startDate: LocalDateTime, endDate: LocalDateTime): String {
        val transactions = transactionDao.getTransactionsByDateRange(startDate, endDate).first()

        return transactions
            .filter { it.isDebit }
            .groupBy { Category.getDefaultCategories().find { x -> x.id==it.categoryId}?.name ?: "Unknown"}
            .maxByOrNull { it.value.size }
            ?.key ?: "No data"
    }

}
