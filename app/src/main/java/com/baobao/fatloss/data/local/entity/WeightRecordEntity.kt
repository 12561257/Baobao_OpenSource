package com.baobao.fatloss.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_record", primaryKeys = ["date"])
data class WeightRecordEntity(
    val date: Long, // epochDay of LocalDate
    val weight: Double,
    val recordedAt: Long = System.currentTimeMillis()
)
