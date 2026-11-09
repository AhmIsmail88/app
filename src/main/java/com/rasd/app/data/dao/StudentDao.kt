package com.rasd.app.data.dao

import androidx.room.*
import com.rasd.app.data.model.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(studentEntity: StudentEntity)
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStudents(studentEntities: List<StudentEntity>)
    
    @Update
    suspend fun updateStudent(studentEntity: StudentEntity)
    
    @Delete
    suspend fun deleteStudent(studentEntity: StudentEntity)
    
    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY arabicName COLLATE LOCALIZED")
    fun getStudentsByClass(classId: Long): Flow<List<StudentEntity>>
    
    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentById(id: Long): StudentEntity?

    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY arabicName COLLATE LOCALIZED")
    suspend fun getStudentsByClassOnce(classId: Long): List<StudentEntity>

    @Query("SELECT COUNT(*) FROM students")
    fun countStudents(): Flow<Int>
    
    @Query("SELECT * FROM students WHERE classId = :classId AND studentNumber LIKE '%' || :searchTerm || '%'")
    suspend fun searchStudentsByNumber(classId: Long, searchTerm: String): List<StudentEntity>
    
    @Query("SELECT * FROM students WHERE classId = :classId AND (arabicName LIKE '%' || :searchTerm || '%' OR englishName LIKE '%' || :searchTerm || '%')")
    suspend fun searchStudentsByName(classId: Long, searchTerm: String): List<StudentEntity>
    
    @Query("SELECT * FROM students WHERE id = :studentId")
    suspend fun getStudentByIdentifier(studentId: Long): StudentEntity?
    
    @Query("UPDATE students SET isActive = 0 WHERE id = :id")
    suspend fun archiveStudent(id: Long)
    
    @Query("UPDATE students SET isActive = 1 WHERE id = :id")
    suspend fun restoreStudent(id: Long)
}