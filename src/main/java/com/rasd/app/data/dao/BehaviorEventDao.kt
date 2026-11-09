package com.rasd.app.data.dao

import androidx.room.*
import com.rasd.app.data.model.BehaviorEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BehaviorEventDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBehaviorEvent(behaviorEvent: BehaviorEventEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBehaviorEvents(behaviorEvents: List<BehaviorEventEntity>)
    
    @Update
    suspend fun updateBehaviorEvent(behaviorEvent: BehaviorEventEntity)
    
    @Delete
    suspend fun deleteBehaviorEvent(behaviorEvent: BehaviorEventEntity)
    
    @Query("SELECT * FROM behavior_events WHERE studentId = :studentId ORDER BY date DESC, time DESC")
    fun getStudentBehaviorHistory(studentId: Long): Flow<List<BehaviorEventEntity>>
    
    @Query("SELECT * FROM behavior_events WHERE classId = :classId ORDER BY date DESC")
    fun getClassBehaviorEvents(classId: Long): Flow<List<BehaviorEventEntity>>
    
    @Query("SELECT * FROM behavior_events WHERE studentId = :studentId AND classId = :classId AND date = :date")
    suspend fun getBehaviorsForDate(studentId: Long, classId: Long, date: String): List<BehaviorEventEntity>

    @Query("SELECT COUNT(*) FROM behavior_events WHERE behaviorTypeId = :typeId")
    suspend fun countEventsForType(typeId: Long): Int

    @Query("SELECT * FROM behavior_events WHERE classId = :classId AND date BETWEEN :from AND :to ORDER BY date DESC")
    suspend fun getBehaviorInRange(classId: Long, from: String, to: String): List<BehaviorEventEntity>

    @Query("SELECT * FROM behavior_events WHERE studentId = :studentId AND date BETWEEN :from AND :to ORDER BY date DESC, time DESC")
    suspend fun getStudentBehaviorInRange(studentId: Long, from: String, to: String): List<BehaviorEventEntity>
}