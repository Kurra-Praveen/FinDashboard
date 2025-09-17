package com.kpr.fintrack.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["name"]),
        Index(value = ["accountNumber"], unique = true),
        Index(value = ["bankName"])
    ]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val accountNumber: String,
    val bankName: String,
    val currentBalance: BigDecimal = BigDecimal.ZERO,
    val accountType: String, // SAVINGS, CHECKING, CREDIT, etc.
    val isActive: Boolean = true,
    val icon: String? = null,
    val color: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)