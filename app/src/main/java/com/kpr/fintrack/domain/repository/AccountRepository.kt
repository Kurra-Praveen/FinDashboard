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
}