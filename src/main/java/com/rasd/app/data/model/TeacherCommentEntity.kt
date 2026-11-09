package com.rasd.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "teacher_comments",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TeacherCommentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val studentId: Long,
    
    val classId: Long,
    
    val periodStart: String, // Format: yyyy-MM-dd
    
    val periodEnd: String, // Format: yyyy-MM-dd
    
    val generatedComment: String = "",
    
    val finalComment: String? = null, // Teacher's edited comment
    
    val createdAt: Long = System.currentTimeMillis(),
    
    val updatedAt: Long = System.currentTimeMillis(),
)