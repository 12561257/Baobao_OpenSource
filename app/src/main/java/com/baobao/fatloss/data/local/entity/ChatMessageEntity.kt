package com.baobao.fatloss.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_message")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user" / "assistant"
    val content: String,
    val imagePath: String? = null, // 新增：保存图片本地路径
    val timestamp: Long = System.currentTimeMillis()
)
