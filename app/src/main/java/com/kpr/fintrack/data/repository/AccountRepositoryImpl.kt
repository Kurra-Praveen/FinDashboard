package com.kpr.fintrack.data.repository

import com.kpr.fintrack.data.database.dao.AccountDao
import com.kpr.fintrack.data.database.entities.AccountEntity
import com.kpr.fintrack.domain.model.Account
import com.kpr.fintrack.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getAllActiveAccounts(): Flow<List<Account>> {
        return accountDao.getAllActiveAccounts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAccountById(accountId: Long): Flow<Account?> {
        return accountDao.getAccountById(accountId).map { entity ->
            entity?.toDomain()
        }
    }

    override fun getAccountByNumber(accountNumber: String): Flow<Account?> {
        return accountDao.getAccountByNumber(accountNumber).map { entity ->
            entity?.toDomain()
        }
    }

    override fun getAccountsByBank(bankName: String): Flow<List<Account>> {
        return accountDao.getAccountsByBank(bankName).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertAccount(account: Account): Long {
        return accountDao.insertAccount(account.toEntity())
    }

    override suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account.toEntity())
    }

    override suspend fun updateAccountBalance(accountId: Long, balance: BigDecimal) {
        accountDao.updateAccountBalance(accountId, balance)
    }

    override suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccount(account.toEntity())
    }

    override suspend fun deactivateAccount(accountId: Long) {
        accountDao.deactivateAccount(accountId)
    }

    override suspend fun getAccountCount(): Int {
        return accountDao.getAccountCount()
    }

    private fun AccountEntity.toDomain(): Account {
        return Account(
            id = id,
            name = name,
            accountNumber = accountNumber,
            bankName = bankName,
            currentBalance = currentBalance,
            accountType = Account.AccountType.fromString(accountType),
            isActive = isActive,
            icon = icon,
            color = color,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun Account.toEntity(): AccountEntity {
        return AccountEntity(
            id = id,
            name = name,
            accountNumber = accountNumber,
            bankName = bankName,
            currentBalance = currentBalance,
            accountType = accountType.name,
            isActive = isActive,
            icon = icon,
            color = color,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}