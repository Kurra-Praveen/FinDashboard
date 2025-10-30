package com.kpr.fintrack.domain.repository

import com.kpr.fintrack.domain.model.Account
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.YearMonth

interface AccountRepository {
    fun getAllActiveAccounts(): Flow<List<Account>>
    fun getAllAccounts(): Flow<List<Account>>
    fun getAccountById(accountId: Long): Flow<Account?>
    fun getAccountByNumber(accountNumber: String): Flow<Account?>
    fun getAccountsByBank(bankName: String): Flow<List<Account>>
    suspend fun insertAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    suspend fun updateAccountBalance(accountId: Long, balance: BigDecimal)
    suspend fun deleteAccount(account: Account)
    suspend fun deactivateAccount(accountId: Long)
    suspend fun getAccountCount(): Int
    suspend fun getAccountMonthlyAnalytics(accountId: Long, yearMonth: YearMonth): Account.MonthlyAnalytics

    /**
     * Creates and inserts a new account based on source data.
     * It uses BankUtils to determine the bank name, icon, and color.
     * If sender and messageBody are null, it defaults to a generic "Bank" account.
     */
    suspend fun createAccountFromSource(
        accountNumber: String,
        sender: String? = null,
        messageBody: String? = null
    ): Account

    /**
     * The new master method for Block 3.
     * Fetches an account by its number.
     * - If found and the bankName is "Bank", it attempts to update it with a specific name.
     * - If not found, it calls `createAccountFromSource` to make a new one.
     */
    suspend fun getOrCreateAccount(
        accountNumber: String,
        sender: String? = null,
        messageBody: String? = null
    ): Account
}