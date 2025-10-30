package com.kpr.fintrack.domain.model

import com.kpr.fintrack.utils.parsing.TransactionParser
import java.math.BigDecimal
import java.time.LocalDateTime

data class Transaction(
    val id: Long = 0,
    val amount: BigDecimal,
    val isDebit: Boolean,
    val merchantName: String,
    val description: String,
    val category: Category,
    val date: LocalDateTime,
    val upiApp: UpiApp? = null,
    val account: Account? = null,
    val accountNumber: String? = null,
    val referenceId: String? = null,
    val smsBody: String, // Store original SMS for debugging
    val sender: String,
    val confidence: Float, // Parser confidence score
    val isManuallyVerified: Boolean = false,
    val tags: List<String> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val receiptImagePath: String? = null,
    val receiptSource: String? = null
)

enum class TransactionType {
    DEBIT, CREDIT, ALL
}

/**
 * Factory function to create a Transaction from a ParseResult and related data.
 */
fun createTransactionFromParseResult(
    parseResult: TransactionParser.ParseResult,
    category: Category,
    account: Account?,
    originalText: String,
    sender: String,
    fallbackDescription: String = originalText,
    receiptImagePath: String? = null,
    receiptSource: String? = null
): Transaction ?{

    // The required fields from ParseResult. If missing, we can't create a transaction.
    val amount = parseResult.amount ?: return null
    val isDebit = parseResult.isDebit ?: true // Default to debit if unknown

    // Default date to now if parsing failed
    val date = parseResult.extractedDate ?: LocalDateTime.now()

    return Transaction(
        amount = amount,
        isDebit = isDebit,
        merchantName = parseResult.merchantName ?: "Unknown",
        description = parseResult.description ?: fallbackDescription,
        category = category,
        date = date,
        upiApp = parseResult.upiApp,
        accountNumber = parseResult.accountNumber,
        referenceId = parseResult.referenceId,
        smsBody = originalText, // The raw or cleaned text
        sender = sender,
        confidence = parseResult.confidence,
        account = account,
        receiptImagePath = receiptImagePath,
        receiptSource = receiptSource
    )
}