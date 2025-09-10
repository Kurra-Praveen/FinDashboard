package com.kpr.fintrack.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: String,
    val parentCategoryId: Long? = null,
    val isDefault: Boolean = false,
    val keywords: String = "" // JSON string of keywords
)
