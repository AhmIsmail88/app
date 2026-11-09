package com.rasd.app.repository

import com.rasd.app.data.dao.ClassDao
import com.rasd.app.data.dao.StudentDao
import com.rasd.app.data.model.StudentEntity
import kotlinx.coroutines.flow.Flow

class StudentRepository(
    private val studentDao: StudentDao,
    private val classDao: ClassDao
) {
    
    suspend fun getActiveClasses() = classDao.getActiveClasses(false)
    
    fun getStudentsByClass(classId: Long): Flow<List<StudentEntity>> = studentDao.getStudentsByClass(classId)
    
    suspend fun getStudentById(id: Long): StudentEntity? = studentDao.getStudentById(id)
    
    suspend fun insertStudent(student: StudentEntity) = studentDao.insertStudent(student)
    
    suspend fun insertStudents(students: List<StudentEntity>) = studentDao.insertStudents(students)
    
    suspend fun updateStudent(student: StudentEntity) = studentDao.updateStudent(student)
    
    suspend fun deleteStudent(student: StudentEntity) = studentDao.deleteStudent(student)
    
    suspend fun archiveStudent(id: Long) = studentDao.archiveStudent(id)
    
    suspend fun restoreStudent(id: Long) = studentDao.restoreStudent(id)
}