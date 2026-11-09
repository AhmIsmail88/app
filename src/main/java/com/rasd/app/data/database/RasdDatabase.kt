package com.rasd.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rasd.app.data.dao.*
import com.rasd.app.data.model.*

@Database(
    version = 3,
    exportSchema = false,
    entities = [
        ClassEntity::class,
        StudentEntity::class,
        AttendanceRecordEntity::class,
        BehaviorTypeEntity::class,
        BehaviorEventEntity::class,
        AssessmentEntity::class,
        AssessmentScoreEntity::class,
        TeacherCommentEntity::class,
        AppSettingsEntity::class,
        ClassPeriodEntity::class // إضافة الجدول المدرسي
    ]
)
abstract class RasdDatabase : RoomDatabase() {
    
    abstract fun classDao(): ClassDao
    abstract fun studentDao(): StudentDao
    abstract fun attendanceRecordDao(): AttendanceRecordDao
    abstract fun behaviorTypeDao(): BehaviorTypeDao
    abstract fun behaviorEventDao(): BehaviorEventDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun assessmentScoreDao(): AssessmentScoreDao
    abstract fun teacherCommentDao(): TeacherCommentDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun classPeriodDao(): ClassPeriodDao // DAO الجديد للجدول
}