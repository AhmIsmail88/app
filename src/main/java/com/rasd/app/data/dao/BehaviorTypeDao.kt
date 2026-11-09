package com.rasd.app.data.dao

import androidx.room.*
import com.rasd.app.data.model.BehaviorTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BehaviorTypeDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBehaviorType(behaviorType: BehaviorTypeEntity)
    
    @Update
    suspend fun updateBehaviorType(behaviorType: BehaviorTypeEntity)
    
    @Delete
    suspend fun deleteBehaviorType(behaviorType: BehaviorTypeEntity)
    
    @Query("SELECT * FROM behavior_types ORDER BY sortOrder ASC, isActive DESC")
    fun getAllBehaviorTypes(): Flow<List<BehaviorTypeEntity>>
    
    @Query("SELECT * FROM behavior_types WHERE id = :id")
    suspend fun getBehaviorTypeById(id: Long): BehaviorTypeEntity?
    
    @Query("SELECT * FROM behavior_types WHERE isSystemDefault = 1 ORDER BY sortOrder ASC")
    fun getSystemDefaultBehaviorTypes(): Flow<List<BehaviorTypeEntity>>

    @Query("SELECT * FROM behavior_types")
    suspend fun getAllOnce(): List<BehaviorTypeEntity>
}