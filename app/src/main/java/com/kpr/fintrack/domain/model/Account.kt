package com.kpr.fintrack.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class Account(
    val id: Long = 0,
    val name: String,
    val accountNumber: String,
    var bankName: String,
    val currentBalance: BigDecimal = BigDecimal.ZERO,
    val accountType: AccountType,
    val isActive: Boolean = true,
    val icon: String? = null,
    val color: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    
    // Monthly analytics data (calculated dynamically)
    data class MonthlyAnalytics(
        val totalInflow: BigDecimal = BigDecimal.ZERO,
        val totalOutflow: BigDecimal = BigDecimal.ZERO,
        val netFlow: BigDecimal = BigDecimal.ZERO,
        val transactionCount: Int = 0
    ) {
        val isPositive: Boolean get() = netFlow >= BigDecimal.ZERO
    }
    enum class AccountType {
        SAVINGS, CHECKING, CREDIT, WALLET, INVESTMENT, OTHER;
        
        companion object {
            fun fromString(value: String): AccountType {
                return try {
                    valueOf(value.uppercase())
                } catch (e: Exception) {
                    OTHER
                }
            }
        }
    }
    
    companion object {
        fun getDefaultAccount(): Account {
            return Account(
                name = "Default Account",
                accountNumber = "XXXX0000",
                bankName = "Default Bank",
                accountType = AccountType.SAVINGS
            )
        }
    }
}