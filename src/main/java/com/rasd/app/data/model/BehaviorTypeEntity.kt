package com.rasd.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "behavior_types")
data class BehaviorTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val nameArabic: String,
    
    val nameEnglish: String,
    
    val category: String, // positive, negative, neutral
    
    val defaultPoints: Float? = null,
    
    val isSystemDefault: Boolean = false,
    
    val isActive: Boolean = true,
    
    val sortOrder: Int = 0,
)