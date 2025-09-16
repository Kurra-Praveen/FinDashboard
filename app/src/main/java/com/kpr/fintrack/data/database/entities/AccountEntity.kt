package com.kpr.fintrack.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kpr.fintrack.domain.model.Account
import com.kpr.fintrack.domain.model.AccountType
import java.math.BigDecimal

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val balance: BigDecimal
)

fun AccountEntity.toDomain(): Account {
    return Account(
        id = id,
        name = name,
        type = type,
        balance = balance
    )
}

fun Account.toEntity(): AccountEntity {
    return AccountEntity(
        id = id,
        name = name,
        type = type,
        balance = balance
    )
}
