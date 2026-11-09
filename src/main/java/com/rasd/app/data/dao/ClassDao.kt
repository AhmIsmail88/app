package com.rasd.app.data.dao

import androidx.room.*
import com.rasd.app.data.model.ClassEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classEntity: ClassEntity)
    
    @Update
    suspend fun updateClass(classEntity: ClassEntity)
    
    @Delete
    suspend fun deleteClass(classEntity: ClassEntity)
    
    @Query("SELECT * FROM classes WHERE isArchived = :isArchived ORDER BY updatedAt DESC")
    fun getActiveClasses(isArchived: Boolean = false): Flow<List<ClassEntity>>
    
    @Query("SELECT * FROM classes WHERE id = :id")
    suspend fun getClassById(id: Long): ClassEntity?
    
    @Query("SELECT * FROM classes WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getAllActiveClasses(): Flow<List<ClassEntity>>
    
    @Query("SELECT * FROM classes WHERE name LIKE '%' || :searchTerm || '%' OR grade LIKE '%' || :searchTerm || '%'")
    suspend fun searchClasses(searchTerm: String): List<ClassEntity>
    
    @Query("DELETE FROM classes WHERE id = :id")
    suspend fun deleteClassById(id: Long)
}