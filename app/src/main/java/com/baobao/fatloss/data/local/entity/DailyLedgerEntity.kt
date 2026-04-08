package com.baobao.fatloss.data.local.entity

import androidx.room.Entity

@Entity(tableName = "daily_ledger", primaryKeys = ["date"])
data class DailyLedgerEntity(
    val date: Long, // epochDay of LocalDate
    val dailyBudget: Double = 1850.0,
    val consumedCalories: Double = 0.0,
    val burnedCalories: Double = 0.0,
    val waterMl: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val netRemaining: Double get() = dailyBudget - consumedCalories + burnedCalories
}
