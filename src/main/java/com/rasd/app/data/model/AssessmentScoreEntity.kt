package com.rasd.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "assessment_scores",
    foreignKeys = [
        ForeignKey(
            entity = AssessmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["assessmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    primaryKeys = ["assessmentId", "studentId"]
)
data class AssessmentScoreEntity(
    // composite PK declared via @Entity(primaryKeys = ...)
    val assessmentId: Long,
    
    val studentId: Long,
    
    val score: Float,
    
    val maximumScore: Float,
    
    val status: String, // completed, not_completed, absent, excused
    
    val note: String = "",
    
    val createdAt: Long = System.currentTimeMillis(),
    
    val updatedAt: Long = System.currentTimeMillis(),
)