package com.rasd.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "behavior_events",
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
data class BehaviorEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val studentId: Long,
    
    val classId: Long,
    
    val behaviorTypeId: Long,
    
    val date: String, // Format: yyyy-MM-dd
    
    val time: Int? = null, // Hour of day (0-23)
    
    val points: Float? = null,
    
    val note: String = "",
    
    val createdAt: Long = System.currentTimeMillis(),
    
    val updatedAt: Long = System.currentTimeMillis(),
)