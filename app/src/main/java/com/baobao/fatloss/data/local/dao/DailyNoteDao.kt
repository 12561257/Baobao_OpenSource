package com.baobao.fatloss.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.baobao.fatloss.data.local.entity.DailyNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyNoteDao {
    @Query("SELECT * FROM daily_note WHERE date = :epochDay")
    suspend fun getByDate(epochDay: Long): DailyNoteEntity?

    @Query("SELECT * FROM daily_note ORDER BY date DESC LIMIT :limit")
    fun getRecentNotes(limit: Int = 7): Flow<List<DailyNoteEntity>>

    @Query("SELECT * FROM daily_note ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentNotesOnce(limit: Int = 7): List<DailyNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: DailyNoteEntity)

    @Query("DELETE FROM daily_note WHERE date = :epochDay")
    suspend fun deleteByDate(epochDay: Long)

    @Query("DELETE FROM daily_note")
    suspend fun clearAll()
}
