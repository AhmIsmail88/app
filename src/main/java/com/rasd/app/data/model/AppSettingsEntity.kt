package com.rasd.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 1,
    
    val language: String = "ar", // ar or en
    
    val theme: String = "light", // light, dark, system
    
    val schoolName: String = "",
    
    val teacherName: String = "",
    
    val academicYear: String = "",
    
    val logoPath: String = "",
    
    val defaultAttendanceStatus: String = "not_recorded",
    
    val enabledAttendanceStatuses: String = "present,absent,late,excused,permission",
    
    val behaviorCommentRules: String = "", // JSON rules
    
    val reportHeader: String = "رصد - منصة التقدير والأداء الوظيفي",
    
    val reportFooter: String = "تم إعداد هذا التقرير بواسطة تطبيق رصد",
    
    val createdAt: Long = System.currentTimeMillis(),
)