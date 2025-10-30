package com.kpr.fintrack.data.repository

import android.util.Log
import com.kpr.fintrack.data.database.dao.AccountDao
import com.kpr.fintrack.data.database.dao.TransactionDao
import com.kpr.fintrack.data.database.entities.AccountEntity
import com.kpr.fintrack.domain.model.Account
import com.kpr.fintrack.domain.repository.AccountRepository
import com.kpr.fintrack.utils.logging.SecureLogger
import com.kpr.fintrack.utils.parsing.BankUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val secureLogger: SecureLogger
) : AccountRepository {

    init {
        android.util.Log.d("AccountRepositoryImpl", "Initialized")
    }

    override fun getAllActiveAccounts(): Flow<List<Account>> {
        android.util.Log.d("AccountRepositoryImpl", "getAllActiveAccounts called")
        return accountDao.getAllActiveAccounts().map { entities ->
            android.util.Log.d("AccountRepositoryImpl", "Fetched active accounts: ${entities.size}")
            entities.map { it.toDomain() }
        }
    }

    override fun getAllAccounts(): Flow<List<Account>> {
        android.util.Log.d("AccountRepositoryImpl", "getAllAccounts called")
        return accountDao.getAllAccounts().map { entities ->
            android.util.Log.d("AccountRepositoryImpl", "Fetched all accounts: ${entities.size}")
            entities.map { it.toDomain() }
        }
    }

    override fun getAccountById(accountId: Long): Flow<Account?> {
        android.util.Log.d("AccountRepositoryImpl", "getAccountById called: $accountId")
        return accountDao.getAccountById(accountId).map { entity ->
            android.util.Log.d(
                "AccountRepositoryImpl",
                "Fetched account by id: $accountId, found: ${entity != null}"
            )
            entity?.toDomain()
        }
    }

    override fun getAccountByNumber(accountNumber: String): Flow<Account?> {
        android.util.Log.d("AccountRepositoryImpl", "getAccountByNumber called: $accountNumber")
        return accountDao.getAccountByNumber(accountNumber).map { entity ->
            android.util.Log.d(
                "AccountRepositoryImpl",
                "Fetched account by number: $accountNumber, found: ${entity != null}"
            )
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

    override suspend fun getAccountMonthlyAnalytics(
        accountId: Long, yearMonth: YearMonth
    ): Account.MonthlyAnalytics {
        val startOfMonth = yearMonth.atDay(1).atStartOfDay()
        val endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59)

        val transactions = transactionDao.getTransactionsByAccountIdAndDateRange(
            accountId, startOfMonth, endOfMonth
        )

        val totalInflow = transactions.filter { !it.isDebit }.sumOf { it.amount }
        val totalOutflow = transactions.filter { it.isDebit }.sumOf { it.amount }
        val netFlow = totalInflow.subtract(totalOutflow)

        return Account.MonthlyAnalytics(
            totalInflow = totalInflow,
            totalOutflow = totalOutflow,
            netFlow = netFlow,
            transactionCount = transactions.size
        )
    }

    override suspend fun createAccountFromSource(
        accountNumber: String, sender: String?, messageBody: String?
    ): Account {
        // Use the BankUtils from your Block 1 refactor
        val bankName = BankUtils.extractBankNameFromSms(sender ?: "", messageBody ?: "")

        val accountName = if (accountNumber.length >= 4) {
            "$bankName ****${accountNumber.takeLast(4)}"
        } else {
            "$bankName Account"
        }

        val newAccount = Account(
            name = accountName,
            accountNumber = accountNumber,
            bankName = bankName,
            accountType = Account.AccountType.SAVINGS, // Default to SAVINGS
            isActive = true,
            icon = BankUtils.getBankIcon(bankName),
            color = BankUtils.getBankColor(bankName)
        )
        val accountId = accountDao.insertAccount(newAccount.toEntity())
        secureLogger.i("AccountRepository", "Created new account: $accountName with ID: $accountId")
        return newAccount.copy(id = accountId)
    }

    override suspend fun getOrCreateAccount(
        accountNumber: String,
        sender: String?,
        messageBody: String?
    ): Account {
        val existingAccount = accountDao.getAccountByNumber(accountNumber).firstOrNull()

        if (existingAccount != null) {
            if (existingAccount.bankName.equals(
                    "Bank",
                    ignoreCase = true
                ) && sender != null && messageBody != null
            ) {
                val specificBankName = BankUtils.extractBankNameFromSms(sender, messageBody)

                if (specificBankName != "Bank") {
                    val updatedAccount = existingAccount.copy(
                        bankName = specificBankName,
                        updatedAt = LocalDateTime.now(),
                        name = existingAccount.name.replace(
                            "Bank",
                            specificBankName,
                            ignoreCase = true
                        ).trim()
                    )

                    accountDao.updateAccount(updatedAccount) // Assumes you have a .toEntity() mapper
                    Log.i("AccountRepository", "Updated  account $updatedAccount ")
                    Log.i(
                        "AccountRepository",
                        "From generic bank to specific bank updated one: ${
                            getAccountById(updatedAccount.id)
                        }"
                    )
                    secureLogger.i(
                        "AccountRepository",
                        "Updated generic account $accountNumber to $specificBankName"
                    )

                    return updatedAccount.toDomain()
                }
            }
            return existingAccount.toDomain()
        } else {
            secureLogger.i(
                "AccountRepository",
                "Account $accountNumber not found, creating new one."
            )
            return createAccountFromSource(accountNumber, sender, messageBody)
        }
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

    fun Account.toEntity(): AccountEntity {
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