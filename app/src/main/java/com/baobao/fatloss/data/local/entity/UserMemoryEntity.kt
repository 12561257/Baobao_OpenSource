package com.baobao.fatloss.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_memory")
data class UserMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,    // "preference" | "observation" | "habit" | "goal" | "custom"
    val content: String,
    val source: String,      // "ai" | "user" | "system"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val embedding: String? = null // 存储 FloatArray 生成的特征向量 JSON (例如 "[0.123, -0.456...]")
)
