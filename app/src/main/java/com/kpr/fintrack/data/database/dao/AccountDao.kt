package com.kpr.fintrack.data.database.dao

import androidx.room.*
import com.kpr.fintrack.data.database.entities.AccountEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveAccounts(): Flow<List<AccountEntity>>
    
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>
    
    @Query("SELECT * FROM accounts WHERE id = :accountId")
    fun getAccountById(accountId: Long): Flow<AccountEntity?>
    
    @Query("SELECT * FROM accounts WHERE accountNumber = :accountNumber")
    fun getAccountByNumber(accountNumber: String): Flow<AccountEntity?>
    
    @Query("SELECT * FROM accounts WHERE bankName = :bankName")
    fun getAccountsByBank(bankName: String): Flow<List<AccountEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>): List<Long>
    
    @Update
    suspend fun updateAccount(account: AccountEntity)
    
    @Query("UPDATE accounts SET currentBalance = :balance, updatedAt = datetime('now') WHERE id = :accountId")
    suspend fun updateAccountBalance(accountId: Long, balance: BigDecimal)
    
    @Delete
    suspend fun deleteAccount(account: AccountEntity)
    
    @Query("UPDATE accounts SET isActive = 0 WHERE id = :accountId")
    suspend fun deactivateAccount(accountId: Long)
    
    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int
}