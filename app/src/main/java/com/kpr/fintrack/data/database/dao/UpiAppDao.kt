package com.kpr.fintrack.data.database.dao

import androidx.room.*
import com.kpr.fintrack.data.database.entities.UpiAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UpiAppDao {

    @Query("SELECT * FROM upi_apps ORDER BY name ASC")
    fun getAllUpiApps(): Flow<List<UpiAppEntity>>

    @Query("SELECT * FROM upi_apps WHERE id = :id")
    suspend fun getUpiAppById(id: Long): UpiAppEntity?

    @Query("SELECT * FROM upi_apps WHERE senderPattern = :senderPattern LIMIT 1")
    suspend fun getUpiAppBySenderPattern(senderPattern: String): UpiAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpiApp(upiApp: UpiAppEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpiApps(upiApps: List<UpiAppEntity>)

    @Update
    suspend fun updateUpiApp(upiApp: UpiAppEntity)

    @Delete
    suspend fun deleteUpiApp(upiApp: UpiAppEntity)
}
