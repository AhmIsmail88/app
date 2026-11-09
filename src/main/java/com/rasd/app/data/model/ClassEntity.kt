package com.rasd.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    
    val grade: String = "",
    
    val section: String = "",
    
    val subject: String = "",
    
    val academicYear: String = "",
    
    val teacherName: String = "",
    
    val createdAt: Long = System.currentTimeMillis(),
    
    val updatedAt: Long = System.currentTimeMillis(),
    
    val isArchived: Boolean = false,
)