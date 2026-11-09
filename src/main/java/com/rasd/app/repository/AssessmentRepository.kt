package com.rasd.app.repository

import com.rasd.app.data.dao.AssessmentDao
import com.rasd.app.data.model.AssessmentEntity
import kotlinx.coroutines.flow.Flow

class AssessmentRepository(
    private val assessmentDao: AssessmentDao
) {
    
    fun getAssessmentsByClass(classId: Long): Flow<List<AssessmentEntity>> = 
        assessmentDao.getAssessmentsByClass(classId)
}