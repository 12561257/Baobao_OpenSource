package com.baobao.fatloss.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.baobao.fatloss.data.local.entity.UserMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserMemoryDao {
    @Query("SELECT * FROM user_memory ORDER BY updatedAt DESC")
    fun getAllMemories(): Flow<List<UserMemoryEntity>>

    @Query("SELECT * FROM user_memory ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentMemories(limit: Int): List<UserMemoryEntity>

    @Query("SELECT * FROM user_memory ORDER BY category ASC, updatedAt DESC")
    suspend fun getAllMemoriesList(): List<UserMemoryEntity>

    @Query("SELECT * FROM user_memory WHERE category = :cat ORDER BY updatedAt DESC")
    fun getMemoriesByCategory(cat: String): Flow<List<UserMemoryEntity>>

    @Insert suspend fun insert(memory: UserMemoryEntity): Long
    @Update suspend fun update(memory: UserMemoryEntity)
    
    @Query("UPDATE user_memory SET content = :content, embedding = :embedding, updatedAt = :now WHERE id = :id")
    suspend fun updateById(id: Long, content: String, embedding: String?, now: Long)
    
    @Delete suspend fun delete(memory: UserMemoryEntity)
    
    @Query("DELETE FROM user_memory WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM user_memory") suspend fun clearAll()
}
