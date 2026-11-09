package com.rasd.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("studentNumber"),
        Index("arabicName")
    ]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val classId: Long,
    
    val studentNumber: String,
    
    val arabicName: String,
    
    val englishName: String,
    
    val schoolId: String? = null,
    
    val parentName: String = "",
    
    val parentPhone: String = "",
    
    val notes: String = "",
    
    val isActive: Boolean = true,
    
    val createdAt: Long = System.currentTimeMillis(),
    
    val updatedAt: Long = System.currentTimeMillis(),
)