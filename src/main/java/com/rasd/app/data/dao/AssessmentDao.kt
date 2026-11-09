package com.rasd.app.data.dao

import androidx.room.*
import com.rasd.app.data.model.AssessmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessment: AssessmentEntity)
    
    @Update
    suspend fun updateAssessment(assessment: AssessmentEntity)
    
    @Delete
    suspend fun deleteAssessment(assessment: AssessmentEntity)
    
    @Query("SELECT * FROM assessments WHERE classId = :classId ORDER BY date DESC")
    fun getAssessmentsByClass(classId: Long): Flow<List<AssessmentEntity>>
    
    @Query("SELECT * FROM assessments WHERE id = :id")
    suspend fun getAssessmentById(id: Long): AssessmentEntity?

    @Query("SELECT * FROM assessments WHERE classId = :classId AND date BETWEEN :from AND :to ORDER BY date DESC")
    suspend fun getAssessmentsInRange(classId: Long, from: String, to: String): List<AssessmentEntity>
}