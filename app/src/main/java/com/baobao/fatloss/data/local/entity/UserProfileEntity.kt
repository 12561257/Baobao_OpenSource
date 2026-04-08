package com.baobao.fatloss.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val userId: String = "default_user",
    val name: String = "",
    val heightCm: Int = 170,
    val initialWeight: Double = 79.0,
    val currentWeight: Double = 79.0,
    val targetWeight: Double = 70.0,
    val targetDate: Long = 0L, // epochDay of LocalDate
    val activityLevel: Int = 2, // 1=久坐 2=轻度 3=中度 4=重度
    val age: Int = 22,
    val gender: Int = 1, // 1=男 2=女
    val baseBmr: Double = 0.0,
    val baseTdee: Double = 0.0,
    val dailyBudget: Double = 0.0,
    val aiPersona: String = "", // 空表示官方默认温柔人设
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
