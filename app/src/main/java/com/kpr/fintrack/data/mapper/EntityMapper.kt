package com.kpr.fintrack.data.mapper

import com.kpr.fintrack.data.database.dao.AccountDao
import com.kpr.fintrack.data.database.entities.AccountEntity
import com.kpr.fintrack.data.database.entities.CategoryEntity
import com.kpr.fintrack.data.database.entities.TransactionEntity
import com.kpr.fintrack.data.database.entities.UpiAppEntity
import com.kpr.fintrack.domain.model.Account
import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.model.Transaction
import com.kpr.fintrack.domain.model.UpiApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

suspend fun TransactionEntity.toDomainModel(accountDao: AccountDao): Transaction = withContext(Dispatchers.IO) {
    val defaultCategory = Category.getDefaultCategories().find { it.id == this@toDomainModel.categoryId }
        ?: Category.getDefaultCategories().last() // Default to "Other"

    val account = this@toDomainModel.accountId?.let { id ->
        accountDao.getAccountById(id).first()?.toDomainModel()
    }

    Transaction(
        id = id,
        amount = amount,
        isDebit = isDebit,
        merchantName = merchantName,
        description = description,
        category = defaultCategory,
        date = date,
        upiApp = null,
        account = account,
        accountNumber = accountNumber,
        referenceId = referenceId,
        smsBody = smsBody,
        sender = sender,
        confidence = confidence,
        isManuallyVerified = isManuallyVerified,
        tags = tags.split(",").filter { it.isNotBlank() },
        createdAt = createdAt,
        updatedAt = updatedAt,
        receiptImagePath = this@toDomainModel.receiptImagePath,
        receiptSource = this@toDomainModel.receiptSource
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        amount = amount,
        isDebit = isDebit,
        merchantName = merchantName,
        description = description,
        categoryId = category.id,
        date = date,
        upiAppId = upiApp?.id,
        accountId = account?.id,
        accountNumber = accountNumber,
        referenceId = referenceId,
        smsBody = smsBody,
        sender = sender,
        confidence = confidence,
        isManuallyVerified = isManuallyVerified,
        tags = tags.joinToString(","),
        createdAt = createdAt,
        updatedAt = updatedAt,
        receiptImagePath = receiptImagePath,
        receiptSource = receiptSource
    )
}

fun CategoryEntity.toDomainModel(): Category {
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

fun Category.toEntity(): CategoryEntity {
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

fun UpiAppEntity.toDomainModel(): UpiApp {
    return UpiApp(
        id = id,
        name = name,
        packageName = packageName,
        senderPattern = senderPattern,
        icon = icon
    )
}

fun AccountEntity.toDomainModel(): Account {
    return Account(
        id = id,
        name = name,
        accountNumber = accountNumber,
        bankName = bankName,
        currentBalance = currentBalance,
        accountType = Account.AccountType.fromString(accountType),
        isActive = isActive,
        icon = icon,
        color = color,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
