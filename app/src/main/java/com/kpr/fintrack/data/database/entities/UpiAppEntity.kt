package com.kpr.fintrack.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upi_apps")
data class UpiAppEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val packageName: String,
    val senderPattern: String,
    val icon: String
)
