package com.rasd.app.repository

import com.rasd.app.data.dao.TeacherCommentDao
import com.rasd.app.data.model.TeacherCommentEntity
import kotlinx.coroutines.flow.Flow

class TeacherCommentRepository(
    private val teacherCommentDao: TeacherCommentDao
) {
    
    suspend fun insertTeacherComment(comment: TeacherCommentEntity) = 
        teacherCommentDao.insertTeacherComment(comment)
    
    suspend fun getLatestComment(studentId: Long, classId: Long): TeacherCommentEntity? = 
        teacherCommentDao.getLatestComment(studentId, classId)
}
