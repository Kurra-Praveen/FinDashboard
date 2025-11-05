package com.kpr.fintrack.data.database.dto

import androidx.room.Embedded
import androidx.room.Relation
import com.kpr.fintrack.data.database.entities.AccountEntity
import com.kpr.fintrack.data.database.entities.CategoryEntity
import com.kpr.fintrack.data.database.entities.TransactionEntity

/**
 * This class is a data holder for a Transaction and its related
 * Category and Account. Room will populate this for us.
 */
data class TransactionWithDetails(
    @Embedded
    val transaction: TransactionEntity,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?, // Nullable in case of data integrity issues

    @Relation(
        parentColumn = "accountId",
        entityColumn = "id"
    )
    val account: AccountEntity? // Nullable as a transaction might not have an account
)