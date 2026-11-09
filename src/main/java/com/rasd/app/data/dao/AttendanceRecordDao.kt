package com.rasd.app.data.dao

import androidx.room.*
import com.rasd.app.data.model.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceRecordDao {
    
    @Insert(onConflict = OnConflictStrategy.FAIL) // Fail on duplicate to prevent accidental overwrites
    suspend fun insertAttendance(attendanceRecord: AttendanceRecordEntity)
    
    @Update
    suspend fun updateAttendance(attendanceRecord: AttendanceRecordEntity)
    
    @Delete
    suspend fun deleteAttendance(attendanceRecord: AttendanceRecordEntity)
    
    @Query("SELECT * FROM attendance_records WHERE classId = :classId AND date = :date")
    fun getAttendanceByClassAndDate(classId: Long, date: String): Flow<List<AttendanceRecordEntity>>
    
    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND classId = :classId AND date = :date")
    suspend fun getAttendanceForStudent(studentId: Long, classId: Long, date: String): AttendanceRecordEntity?
    
    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND classId = :classId ORDER BY date DESC LIMIT 10")
    fun getStudentAttendanceHistory(studentId: Long, classId: Long): Flow<List<AttendanceRecordEntity>>
    
    @Query("UPDATE attendance_records SET status = :status, note = :note WHERE studentId = :studentId AND classId = :classId AND date = :date")
    suspend fun updateAttendanceStatus(studentId: Long, classId: Long, date: String, status: String, note: String = "")
    
    @Query("SELECT COUNT(*) FROM attendance_records WHERE classId = :classId AND date = :date")
    suspend fun getAttendanceCount(classId: Long, date: String): Int
    
    @Query("SELECT SUM(CASE WHEN status = 'present' THEN 1 ELSE 0 END) FROM attendance_records WHERE classId = :classId AND date = :date")
    suspend fun getPresentCount(classId: Long, date: String): Int
    
    @Query("SELECT SUM(CASE WHEN status = 'absent' THEN 1 ELSE 0 END) FROM attendance_records WHERE classId = :classId AND date = :date")
    suspend fun getAbsentCount(classId: Long, date: String): Int
    
    @Query("SELECT COUNT(*) FROM attendance_records WHERE studentId = :studentId AND classId = :classId AND date BETWEEN :startDate AND :endDate AND status IN ('present', 'absent', 'late')")
    suspend fun getAttendanceDays(studentId: Long, classId: Long, startDate: String, endDate: String): Int
    
    @Query("SELECT SUM(CASE WHEN status = 'present' THEN 1 ELSE 0 END) FROM attendance_records WHERE studentId = :studentId AND classId = :classId AND date BETWEEN :startDate AND :endDate")
    suspend fun getPresentDays(studentId: Long, classId: Long, startDate: String, endDate: String): Int
    
    @Query("SELECT SUM(CASE WHEN status = 'absent' OR status = 'late' THEN 1 ELSE 0 END) FROM attendance_records WHERE studentId = :studentId AND classId = :classId AND date BETWEEN :startDate AND :endDate")
    suspend fun getAbsentDays(studentId: Long, classId: Long, startDate: String, endDate: String): Int
    
    @Query("SELECT * FROM attendance_records WHERE classId = :classId ORDER BY date DESC LIMIT 31")
    fun getRecentAttendance(classId: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE classId = :classId AND date = :date")
    suspend fun getForClassDate(classId: Long, date: String): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE classId = :classId AND date BETWEEN :from AND :to")
    suspend fun getAttendanceInRange(classId: Long, from: String, to: String): List<AttendanceRecordEntity>
}