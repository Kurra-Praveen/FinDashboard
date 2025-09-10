package com.kpr.fintrack.data.repository

import com.kpr.fintrack.data.database.dao.CategoryDao
import com.kpr.fintrack.data.database.dao.TransactionDao
import com.kpr.fintrack.data.database.dao.UpiAppDao
import com.kpr.fintrack.data.database.entities.CategoryEntity
import com.kpr.fintrack.data.database.entities.TransactionEntity
import com.kpr.fintrack.data.database.entities.UpiAppEntity
import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.model.Transaction
import com.kpr.fintrack.domain.model.UpiApp
import com.kpr.fintrack.domain.repository.TransactionFilter
import com.kpr.fintrack.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val upiAppDao: UpiAppDao
) : TransactionRepository {
        init {
            android.util.Log.d("TransactionRepositoryImpl", "Repository initialized")
        }

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTransactionsByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(categoryId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun searchTransactions(query: String): Flow<List<Transaction>> {
        return transactionDao.searchTransactions(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getFilteredTransactions(filter: TransactionFilter): Flow<List<Transaction>> {
        return transactionDao.getFilteredTransactions(
            categoryIds = filter.categoryIds,
            startDate = filter.startDate,
            endDate = filter.endDate,
            minAmount = filter.minAmount,
            maxAmount = filter.maxAmount,
            isDebit = filter.isDebit,
            searchQuery = filter.searchQuery
        ).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> {
        return transactionDao.getRecentTransactions(limit).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction.toEntity())
    }

    override suspend fun insertTransactions(transactions: List<Transaction>) {
        transactionDao.insertTransactions(transactions.map { it.toEntity() })
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction.toEntity())
    }

    override suspend fun getTransactionByReferenceId(referenceId: String): Transaction? {
        return transactionDao.getTransactionByReferenceId(referenceId)?.toDomainModel()
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
}

// Extension functions for entity conversion
private suspend fun TransactionEntity.toDomainModel(): Transaction {
    // Note: In a real implementation, you'd want to fetch the category and upiApp from their DAOs
    val defaultCategory = Category.getDefaultCategories().find { it.id == this.categoryId }
        ?: Category.getDefaultCategories().last() // Default to "Other"

    return Transaction(
        id = id,
        amount = amount,
        isDebit = isDebit,
        merchantName = merchantName,
        description = description,
        category = defaultCategory,
        date = date,
        upiApp = null, // TODO: Fetch from UpiAppDao
        accountNumber = accountNumber,
        referenceId = referenceId,
        smsBody = smsBody,
        sender = sender,
        confidence = confidence,
        isManuallyVerified = isManuallyVerified,
        tags = tags.split(",").filter { it.isNotBlank() },
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        amount = amount,
        isDebit = isDebit,
        merchantName = merchantName,
        description = description,
        categoryId = category.id,
        date = date,
        upiAppId = upiApp?.id,
        accountNumber = accountNumber,
        referenceId = referenceId,
        smsBody = smsBody,
        sender = sender,
        confidence = confidence,
        isManuallyVerified = isManuallyVerified,
        tags = tags.joinToString(","),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun CategoryEntity.toDomainModel(): Category {
    return Category(
        id = id,
        name = name,
        icon = icon,
        color = color,
        parentCategoryId = parentCategoryId,
        isDefault = isDefault,
        keywords = keywords.split(",").filter { it.isNotBlank() }
    )
}

private fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        icon = icon,
        color = color,
        parentCategoryId = parentCategoryId,
        isDefault = isDefault,
        keywords = keywords.joinToString(",")
    )
}

private fun UpiAppEntity.toDomainModel(): UpiApp {
    return UpiApp(
        id = id,
        name = name,
        packageName = packageName,
        senderPattern = senderPattern,
        icon = icon
    )
}
