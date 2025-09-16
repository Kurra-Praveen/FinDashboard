package com.kpr.fintrack.domain.model

import java.math.BigDecimal

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val balance: BigDecimal
)

enum class AccountType {
    BANK,
    CREDIT_CARD,
    WALLET
}
