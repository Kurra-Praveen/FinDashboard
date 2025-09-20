package com.kpr.fintrack.data.database.dao

import androidx.room.*
import com.kpr.fintrack.data.database.entities.AccountEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveAccounts(): Flow<List<AccountEntity>> {
        android.util.Log.d("AccountDao", "getAllActiveAccounts called")
        return getAllActiveAccountsInternal()
    }
    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveAccountsInternal(): Flow<List<AccountEntity>>
    
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>> {
        android.util.Log.d("AccountDao", "getAllAccounts called")
        return getAllAccountsInternal()
    }
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccountsInternal(): Flow<List<AccountEntity>>
    
    @Query("SELECT * FROM accounts WHERE id = :accountId")
    fun getAccountById(accountId: Long): Flow<AccountEntity?> {
        android.util.Log.d("AccountDao", "getAccountById called with id: $accountId")
        return getAccountByIdInternal(accountId)
    }
    @Query("SELECT * FROM accounts WHERE id = :accountId")
    fun getAccountByIdInternal(accountId: Long): Flow<AccountEntity?>
    
    @Query("SELECT * FROM accounts WHERE accountNumber = :accountNumber")
    fun getAccountByNumber(accountNumber: String): Flow<AccountEntity?> {
        android.util.Log.d("AccountDao", "getAccountByNumber called with number: $accountNumber")
        return getAccountByNumberInternal(accountNumber)
    }
    @Query("SELECT * FROM accounts WHERE accountNumber = :accountNumber")
    fun getAccountByNumberInternal(accountNumber: String): Flow<AccountEntity?>
    
    @Query("SELECT * FROM accounts WHERE bankName = :bankName")
    fun getAccountsByBank(bankName: String): Flow<List<AccountEntity>> {
        android.util.Log.d("AccountDao", "getAccountsByBank called with bank: $bankName")
        return getAccountsByBankInternal(bankName)
    }
    @Query("SELECT * FROM accounts WHERE bankName = :bankName")
    fun getAccountsByBankInternal(bankName: String): Flow<List<AccountEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long {
        android.util.Log.d("AccountDao", "insertAccount called: $account")
        return insertAccountInternal(account)
    }
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountInternal(account: AccountEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>): List<Long> {
        android.util.Log.d("AccountDao", "insertAccounts called: $accounts")
        return insertAccountsInternal(accounts)
    }
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountsInternal(accounts: List<AccountEntity>): List<Long>
    
    @Update
    suspend fun updateAccount(account: AccountEntity) {
        android.util.Log.d("AccountDao", "updateAccount called: $account")
        updateAccountInternal(account)
    }
    @Update
    suspend fun updateAccountInternal(account: AccountEntity)
    
    @Query("UPDATE accounts SET currentBalance = :balance, updatedAt = datetime('now') WHERE id = :accountId")
    suspend fun updateAccountBalance(accountId: Long, balance: BigDecimal) {
        android.util.Log.d("AccountDao", "updateAccountBalance called: id=$accountId, balance=$balance")
        updateAccountBalanceInternal(accountId, balance)
    }
    @Query("UPDATE accounts SET currentBalance = :balance, updatedAt = datetime('now') WHERE id = :accountId")
    suspend fun updateAccountBalanceInternal(accountId: Long, balance: BigDecimal)
    
    @Delete
    suspend fun deleteAccount(account: AccountEntity) {
        android.util.Log.d("AccountDao", "deleteAccount called: $account")
        deleteAccountInternal(account)
    }
    @Delete
    suspend fun deleteAccountInternal(account: AccountEntity)
    
    @Query("UPDATE accounts SET isActive = 0 WHERE id = :accountId")
    suspend fun deactivateAccount(accountId: Long) {
        android.util.Log.d("AccountDao", "deactivateAccount called: id=$accountId")
        deactivateAccountInternal(accountId)
    }
    @Query("UPDATE accounts SET isActive = 0 WHERE id = :accountId")
    suspend fun deactivateAccountInternal(accountId: Long)
    
    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int
}