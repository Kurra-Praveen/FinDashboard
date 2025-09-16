package com.kpr.fintrack.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ForeignKey
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["date"]),
        Index(value = ["categoryId"]),
        Index(value = ["accountId"]),
        Index(value = ["merchantName"]),
        Index(value = ["isDebit"]),
        Index(value = ["referenceId"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: BigDecimal,
    val isDebit: Boolean,
    val merchantName: String,
    val description: String,
    val categoryId: Long,
    val accountId: Long?,
    val date: LocalDateTime,
    val upiAppId: Long? = null,
    val accountNumber: String? = null,
    val referenceId: String? = null,
    val smsBody: String,
    val sender: String,
    val confidence: Float,
    val isManuallyVerified: Boolean = false,
    val tags: String = "", // JSON string of tags
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
