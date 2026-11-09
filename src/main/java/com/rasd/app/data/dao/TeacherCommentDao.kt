package com.rasd.app.data.dao

import androidx.room.*
import com.rasd.app.data.model.TeacherCommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherCommentDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacherComment(comment: TeacherCommentEntity)
    
    @Update
    suspend fun updateTeacherComment(comment: TeacherCommentEntity)
    
    @Delete
    suspend fun deleteTeacherComment(comment: TeacherCommentEntity)
    
    @Query("SELECT * FROM teacher_comments WHERE studentId = :studentId AND classId = :classId ORDER BY periodStart DESC LIMIT 1")
    suspend fun getLatestComment(studentId: Long, classId: Long): TeacherCommentEntity?
}