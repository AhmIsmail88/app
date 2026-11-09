package com.rasd.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "class_periods")
data class ClassPeriodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val classId: Long,
    
    val periodNumber: Int, // الحصة الأولى، الثانية، الخ
    
    val dayOfWeek: String, // السبت، الأحد، ...
    
    val startTime: Int, // ساعة من اليوم (0-23)
    
    val endTime: Int, // ساعة من اليوم (0-23)
    
    val startDate: String, // yyyy-MM-DD
    val endDate: String,   // yyyy-MM-DD
    
    val teacherName: String = "",
    
    val isCompleted: Boolean = false, // اكتملت الحصة أم لا
    
    val createdAt: Long = System.currentTimeMillis()
)