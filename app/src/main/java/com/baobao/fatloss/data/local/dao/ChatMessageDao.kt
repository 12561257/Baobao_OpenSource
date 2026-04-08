package com.baobao.fatloss.data.local.dao

import androidx.room.*
import com.baobao.fatloss.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_message ORDER BY id ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_message ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentMessagesOnce(limit: Int): List<ChatMessageEntity>

    @Insert
    suspend fun insertMessage(msg: ChatMessageEntity): Long

    @Query("DELETE FROM chat_message")
    suspend fun clearHistory()
}
