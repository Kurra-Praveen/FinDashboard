package com.kpr.fintrack.domain.model

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
    val accountNumber: String? = null,
    val referenceId: String? = null,
    val smsBody: String, // Store original SMS for debugging
    val sender: String,
    val confidence: Float, // Parser confidence score
    val isManuallyVerified: Boolean = false,
    val tags: List<String> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val receiptImagePath:String? = null
)

enum class TransactionType {
    DEBIT, CREDIT, ALL
}
