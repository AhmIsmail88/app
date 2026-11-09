package com.rasd.app.repository

import com.rasd.app.data.dao.BehaviorEventDao
import com.rasd.app.data.model.BehaviorEventEntity
import kotlinx.coroutines.flow.Flow

class BehaviorEventRepository(
    private val behaviorEventDao: BehaviorEventDao
) {
    
    fun getStudentBehaviorHistory(studentId: Long): Flow<List<BehaviorEventEntity>> = 
        behaviorEventDao.getStudentBehaviorHistory(studentId)
}