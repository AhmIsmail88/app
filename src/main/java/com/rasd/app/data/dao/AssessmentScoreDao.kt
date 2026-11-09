package com.rasd.app.data.dao

import androidx.room.*
import com.rasd.app.data.model.AssessmentScoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentScoreDao {
    
    @Insert(onConflict = OnConflictStrategy.FAIL) // Fail on duplicate to maintain data integrity
    suspend fun insertAssessmentScore(score: AssessmentScoreEntity)
    
    @Update
    suspend fun updateAssessmentScore(score: AssessmentScoreEntity)
    
    @Delete
    suspend fun deleteAssessmentScore(score: AssessmentScoreEntity)
    
    @Query("SELECT * FROM assessment_scores WHERE assessmentId = :assessmentId AND studentId = :studentId")
    suspend fun getScoreForStudent(assessmentId: Long, studentId: Long): AssessmentScoreEntity?

    @Query("SELECT * FROM assessment_scores WHERE assessmentId = :assessmentId")
    suspend fun getScoresForAssessment(assessmentId: Long): List<AssessmentScoreEntity>
    
    @Query("SELECT assessment_scores.* FROM assessment_scores INNER JOIN assessments ON assessment_scores.assessmentId = assessments.id WHERE assessments.classId = :classId ORDER BY assessments.date DESC, assessment_scores.updatedAt DESC")
    fun getClassAssessments(classId: Long): Flow<List<AssessmentScoreEntity>>
    
    @Query("SELECT * FROM assessment_scores WHERE studentId = :studentId AND assessmentId IN (SELECT id FROM assessments WHERE classId = :classId)")
    suspend fun getStudentAllScores(studentId: Long, classId: Long): List<AssessmentScoreEntity>

    @Query("SELECT * FROM assessment_scores WHERE assessmentId IN (SELECT id FROM assessments WHERE classId = :classId AND date BETWEEN :from AND :to)")
    suspend fun getScoresInRange(classId: Long, from: String, to: String): List<AssessmentScoreEntity>
}