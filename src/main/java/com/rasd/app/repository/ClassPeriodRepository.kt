package com.rasd.app.repository

import com.rasd.app.data.dao.ClassPeriodDao
import com.rasd.app.data.model.ClassPeriodEntity
import kotlinx.coroutines.flow.Flow

class ClassPeriodRepository(
    private val classPeriodDao: ClassPeriodDao
) {
    
    suspend fun insertClassPeriod(period: ClassPeriodEntity) = 
        classPeriodDao.insertClassPeriod(period)
    
    suspend fun insertClassPeriods(periods: List<ClassPeriodEntity>) = 
        classPeriodDao.insertClassPeriods(periods)
    
    suspend fun getSchedule(classId: Long, dayOfWeek: String): Flow<List<ClassPeriodEntity>> = 
        classPeriodDao.getClassSchedule(classId, dayOfWeek)
    
    suspend fun getWeeklySchedule(classId: Long, today: String): List<ClassPeriodEntity> = 
        classPeriodDao.getWeeklySchedule(classId, today)
    
    suspend fun getNextClass(classId: Long, dayOfWeek: String): ClassPeriodEntity? = 
        classPeriodDao.getNextClass(classId, dayOfWeek)
}
