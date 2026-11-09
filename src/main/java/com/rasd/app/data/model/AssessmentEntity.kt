package com.rasd.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "assessments",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AssessmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val classId: Long,
    
    val name: String,
    
    val type: String, // quiz, exam, assignment, etc.

    val term: String = "termly", // termly = term work, midyear, final
    
    val date: String, // Format: yyyy-MM-dd
    
    val maximumScore: Float,
    
    val passingScore: Float? = null,
    
    val description: String = "",
    
    val weight: Float? = null,
    
    val academicYear: String = "",
    
    val createdAt: Long = System.currentTimeMillis(),
    
    val updatedAt: Long = System.currentTimeMillis(),
)
