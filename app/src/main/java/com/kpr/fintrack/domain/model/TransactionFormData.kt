package com.kpr.fintrack.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class TransactionFormData(
    val id: Long = 0L,
    val amount: String = "",
    val merchantName: String = "",
    val description: String = "",
    val category: Category = Category.getDefaultCategories().last(),
    val date: LocalDateTime = LocalDateTime.now(),
    val isDebit: Boolean = true,
    val upiApp: UpiApp? = null,
    val account: Account? = null,
    val receiptImagePath: String? = null,
    val tags: List<String> = emptyList(),
    val isManuallyVerified: Boolean = true
) {
    fun toTransaction(): Transaction {
        return Transaction(
            id = id,
            amount = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            isDebit = isDebit,
            merchantName = merchantName.ifBlank { if (isDebit) "Manual Expense" else "Manual Income" },
            description = description.ifBlank { merchantName },
            category = category,
            date = date,
            upiApp = upiApp,
            account = account,
            accountNumber = account?.accountNumber,
            referenceId = "MANUAL_${System.currentTimeMillis()}",
            smsBody = "Manual entry: $description",
            sender = "Manual",
            confidence = 1.0f,
            tags = tags,
            receiptImagePath = receiptImagePath,
            isManuallyVerified = isManuallyVerified,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    companion object {
        fun fromTransaction(transaction: Transaction): TransactionFormData {
            return TransactionFormData(
                id = transaction.id,
                amount = transaction.amount.toPlainString(),
                merchantName = transaction.merchantName,
                description = transaction.description,
                category = transaction.category,
                date = transaction.date,
                isDebit = transaction.isDebit,
                upiApp = transaction.upiApp,
                account = transaction.account,
                receiptImagePath = transaction.receiptImagePath,
                tags = transaction.tags,
                isManuallyVerified = transaction.isManuallyVerified
            )
        }
    }
}

data class QuickTransactionTemplate(
    val id: Long,
    val name: String,
    val merchantName: String,
    val category: Category,
    val isDebit: Boolean,
    val icon: String,
    val suggestedAmount: BigDecimal? = null
) {
    companion object {
        fun getDefaultTemplates(): List<QuickTransactionTemplate> {
            return try {
                val defaultCategories = Category.getDefaultCategories()
                if (defaultCategories.isEmpty()) {
                    android.util.Log.w("QuickTransactionTemplate", "No default categories available")
                    return emptyList()
                }

                // ✅ Safe category lookup with null checks
                val templates = mutableListOf<QuickTransactionTemplate>()

                // Find categories safely and only add templates if category exists
                defaultCategories.find { it.name.contains("Food", ignoreCase = true) }?.let { foodCategory ->
                    templates.add(
                        QuickTransactionTemplate(
                            id = 1,
                            name = "Coffee",
                            merchantName = "Cafe Coffee Day",
                            category = foodCategory,
                            isDebit = true,
                            icon = "☕",
                            suggestedAmount = BigDecimal("150")
                        )
                    )
                }

                defaultCategories.find { it.name.contains("Transport", ignoreCase = true) }?.let { transportCategory ->
                    templates.add(
                        QuickTransactionTemplate(
                            id = 2,
                            name = "Fuel",
                            merchantName = "Petrol Pump",
                            category = transportCategory,
                            isDebit = true,
                            icon = "⛽",
                            suggestedAmount = BigDecimal("2000")
                        )
                    )
                }

                defaultCategories.find { it.name.contains("Shop", ignoreCase = true) }?.let { shoppingCategory ->
                    templates.add(
                        QuickTransactionTemplate(
                            id = 3,
                            name = "Groceries",
                            merchantName = "Supermarket",
                            category = shoppingCategory,
                            isDebit = true,
                            icon = "🛒",
                            suggestedAmount = BigDecimal("800")
                        )
                    )
                }

                defaultCategories.find { it.name.contains("Cash", ignoreCase = true) || it.name.contains("ATM", ignoreCase = true) }?.let { cashCategory ->
                    templates.add(
                        QuickTransactionTemplate(
                            id = 4,
                            name = "ATM Cash",
                            merchantName = "ATM Withdrawal",
                            category = cashCategory,
                            isDebit = true,
                            icon = "🏧",
                            suggestedAmount = BigDecimal("5000")
                        )
                    )
                }

                // Income templates
                defaultCategories.find { it.name.contains("Income", ignoreCase = true) || it.name.contains("Salary", ignoreCase = true) }?.let { incomeCategory ->
                    templates.add(
                        QuickTransactionTemplate(
                            id = 5,
                            name = "Salary",
                            merchantName = "Monthly Salary",
                            category = incomeCategory,
                            isDebit = false,
                            icon = "💰",
                            suggestedAmount = BigDecimal("50000")
                        )
                    )

                    templates.add(
                        QuickTransactionTemplate(
                            id = 6,
                            name = "Interest",
                            merchantName = "Bank Interest",
                            category = incomeCategory,
                            isDebit = false,
                            icon = "🏦",
                            suggestedAmount = BigDecimal("500")
                        )
                    )
                }

                // If no templates were created, provide a fallback
                if (templates.isEmpty() && defaultCategories.isNotEmpty()) {
                    val firstCategory = defaultCategories.first()
                    templates.add(
                        QuickTransactionTemplate(
                            id = 1,
                            name = "Quick Entry",
                            merchantName = "Manual Entry",
                            category = firstCategory,
                            isDebit = true,
                            icon = "💳",
                            suggestedAmount = BigDecimal("100")
                        )
                    )
                }

                android.util.Log.d("QuickTransactionTemplate", "Created ${templates.size} templates")
                templates.toList()

            } catch (e: Exception) {
                android.util.Log.e("QuickTransactionTemplate", "Error creating default templates", e)
                emptyList() // ✅ Return empty list instead of crashing
            }
        }
    }

}
