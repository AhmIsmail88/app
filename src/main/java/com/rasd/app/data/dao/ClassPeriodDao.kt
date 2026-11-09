package com.rasd.app.data.dao

import androidx.room.*
import com.rasd.app.data.model.ClassPeriodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassPeriodDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassPeriod(period: ClassPeriodEntity): Long

    @Query("SELECT * FROM class_periods")
    suspend fun getAllPeriods(): List<ClassPeriodEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassPeriods(periods: List<ClassPeriodEntity>)
    
    @Update
    suspend fun updateClassPeriod(period: ClassPeriodEntity)
    
    @Delete
    suspend fun deleteClassPeriod(period: ClassPeriodEntity)
    
    @Query("SELECT * FROM class_periods WHERE classId = :classId AND dayOfWeek = :dayOfWeek ORDER BY periodNumber ASC")
    fun getClassSchedule(classId: Long, dayOfWeek: String): Flow<List<ClassPeriodEntity>>
    
    @Query("SELECT * FROM class_periods WHERE classId = :classId AND (startDate <= :today OR startDate = '') AND (endDate >= :today OR endDate = '') ORDER BY dayOfWeek ASC, periodNumber ASC")
    suspend fun getWeeklySchedule(classId: Long, today: String): List<ClassPeriodEntity>
    
    @Query("SELECT * FROM class_periods WHERE classId = :classId AND dayOfWeek = :dayOfWeek ORDER BY periodNumber ASC LIMIT 1")
    suspend fun getNextClass(classId: Long, dayOfWeek: String): ClassPeriodEntity?

    @Query("SELECT * FROM class_periods WHERE classId = :classId ORDER BY dayOfWeek ASC, periodNumber ASC")
    suspend fun getPeriodsForClass(classId: Long): List<ClassPeriodEntity>
}
