package com.baobao.fatloss.data.repository

import com.baobao.fatloss.data.local.dao.ChatMessageDao
import com.baobao.fatloss.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

class ChatMessageRepository(private val dao: ChatMessageDao) {
    fun getAllMessages(): Flow<List<ChatMessageEntity>> = dao.getAllMessages()

    suspend fun addUserMessage(content: String): ChatMessageEntity {
        val msg = ChatMessageEntity(role = "user", content = content)
        dao.insertMessage(msg)
        return msg
    }

    suspend fun addAssistantMessage(content: String): ChatMessageEntity {
        val msg = ChatMessageEntity(role = "assistant", content = content)
        dao.insertMessage(msg)
        return msg
    }

    suspend fun clearAll() = dao.clearHistory()
}
