package com.kpr.fintrack.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(
    tableName = "budget_table",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // A category (or null for 'total') can only have one budget per start date
    indices = [Index(value = ["categoryId", "startDate"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Nullable: A null categoryId signifies the "Total Monthly Budget"
    val categoryId: Long?,

    val amount: BigDecimal,

    val period: String = "MONTHLY",

    // We will store the start of the budget period, e.g., 1st of November
    val startDate: Long
)