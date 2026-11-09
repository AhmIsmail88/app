package com.rasd.app.repository

import com.rasd.app.data.dao.ClassDao
import com.rasd.app.data.model.ClassEntity
import kotlinx.coroutines.flow.Flow

class ClassRepository(
    private val classDao: ClassDao
) {
    
    suspend fun insertClass(classEntity: ClassEntity) = classDao.insertClass(classEntity)
    
    suspend fun updateClass(classEntity: ClassEntity) = classDao.updateClass(classEntity)
    
    suspend fun deleteClass(classEntity: ClassEntity) = classDao.deleteClass(classEntity)
    
    fun getActiveClasses(isArchived: Boolean = false): Flow<List<ClassEntity>> = 
        classDao.getActiveClasses(isArchived)
    
    suspend fun getClassById(id: Long): ClassEntity? = classDao.getClassById(id)
}