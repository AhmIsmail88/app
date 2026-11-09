package com.rasd.app.repository

import com.rasd.app.data.dao.AssessmentScoreDao
import com.rasd.app.data.model.AssessmentScoreEntity
import kotlinx.coroutines.flow.Flow

class AssessmentScoreRepository(
    private val assessmentScoreDao: AssessmentScoreDao
) {
    
    suspend fun insertAssessmentScore(score: AssessmentScoreEntity) = 
        assessmentScoreDao.insertAssessmentScore(score)
    
    suspend fun getScoreForStudent(assessmentId: Long, studentId: Long): AssessmentScoreEntity? = 
        assessmentScoreDao.getScoreForStudent(assessmentId, studentId)
}
