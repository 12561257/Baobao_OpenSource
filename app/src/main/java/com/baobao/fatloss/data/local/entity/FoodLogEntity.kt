package com.baobao.fatloss.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_log",
    indices = [Index(value = ["date", "mealType"])]
)
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long, // epochDay of LocalDate
    val mealType: String, // "breakfast" / "lunch" / "dinner" / "snack"
    val foodDescription: String,
    val estimatedCal: Double,
    val carbsG: Double = 0.0,
    val proteinG: Double = 0.0,
    val fatG: Double = 0.0,
    val aiComment: String? = null,
    val inputMethod: String = "text",
    val loggedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun mapToStandardMealType(input: String): String {
            val s = input.lowercase()
            return when {
                s.contains("早") || s.contains("break") -> "breakfast"
                s.contains("午") || s.contains("中") || s.contains("lunch") -> "lunch"
                s.contains("晚") || s.contains("dinner") -> "dinner"
                s.contains("加") || s.contains("零") || s.contains("snack") || s.contains("夜") -> "snack"
                else -> "snack"
            }
        }

        fun getMealLabel(standardType: String): String {
            return when (standardType) {
                "breakfast" -> "早餐"
                "lunch" -> "午餐"
                "dinner" -> "晚餐"
                else -> "加餐"
            }
        }
    }
}
