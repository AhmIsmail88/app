package com.rasd.app.repository

import com.rasd.app.data.dao.AttendanceRecordDao
import com.rasd.app.data.dao.ClassDao
import com.rasd.app.data.dao.StudentDao
import com.rasd.app.data.model.AttendanceRecordEntity
import com.rasd.app.data.model.StudentEntity
import kotlinx.coroutines.flow.Flow

class AttendanceRepository(
    private val studentDao: StudentDao,
    private val classDao: ClassDao,
    private val attendanceRecordDao: AttendanceRecordDao
) {
    
    suspend fun getActiveClasses() = classDao.getActiveClasses(false)
    
    fun getStudentsByClass(classId: Long): Flow<List<StudentEntity>> = studentDao.getStudentsByClass(classId)
    
    suspend fun getAttendanceByClassAndDate(classId: Long, date: String): Flow<List<AttendanceRecordEntity>> = 
        attendanceRecordDao.getAttendanceByClassAndDate(classId, date)
    
    suspend fun getAttendanceForStudent(studentId: Long, classId: Long, date: String): AttendanceRecordEntity? = 
        attendanceRecordDao.getAttendanceForStudent(studentId, classId, date)
    
    suspend fun updateAttendanceStatus(
        studentId: Long, 
        classId: Long, 
        date: String, 
        status: String,
        note: String = ""
    ) = attendanceRecordDao.updateAttendanceStatus(studentId, classId, date, status, note)
}