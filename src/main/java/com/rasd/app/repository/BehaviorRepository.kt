package com.rasd.app.repository

import com.rasd.app.data.dao.BehaviorEventDao
import com.rasd.app.data.dao.BehaviorTypeDao
import com.rasd.app.data.model.BehaviorEventEntity
import com.rasd.app.data.model.BehaviorTypeEntity
import kotlinx.coroutines.flow.Flow

class BehaviorRepository(
    private val behaviorTypeDao: BehaviorTypeDao,
    private val behaviorEventDao: BehaviorEventDao
) {
    
    fun getAllBehaviorTypes(): Flow<List<BehaviorTypeEntity>> = behaviorTypeDao.getAllBehaviorTypes()
    
    suspend fun insertBehaviorType(type: BehaviorTypeEntity) = behaviorTypeDao.insertBehaviorType(type)
    
    suspend fun updateBehaviorType(type: BehaviorTypeEntity) = behaviorTypeDao.updateBehaviorType(type)
    
    suspend fun deleteBehaviorType(type: BehaviorTypeEntity) = behaviorTypeDao.deleteBehaviorType(type)
    
    fun getStudentBehaviorHistory(studentId: Long): Flow<List<BehaviorEventEntity>> = 
        behaviorEventDao.getStudentBehaviorHistory(studentId)
    
    fun getClassBehaviorEvents(classId: Long): Flow<List<BehaviorEventEntity>> = 
        behaviorEventDao.getClassBehaviorEvents(classId)
}