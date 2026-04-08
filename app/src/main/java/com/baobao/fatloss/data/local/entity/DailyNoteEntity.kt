package com.baobao.fatloss.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_note")
data class DailyNoteEntity(
    @PrimaryKey val date: Long,  // LocalDate.toEpochDay()
    val summary: String,
    val calorieSummary: String,
    val aiComment: String,
    val createdAt: Long = System.currentTimeMillis()
)
