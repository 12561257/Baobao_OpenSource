package com.baobao.fatloss.data.repository

import com.baobao.fatloss.data.local.dao.FoodLogDao
import com.baobao.fatloss.data.local.entity.FoodLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class FoodLogRepository(private val dao: FoodLogDao) {
    fun getFoodsByDate(date: LocalDate): Flow<List<FoodLogEntity>> =
        dao.getFoodsByDate(date.toEpochDay())

    fun getFoodsByDateAndMeal(date: LocalDate, mealType: String): Flow<List<FoodLogEntity>> =
        dao.getFoodsByDateAndMeal(date.toEpochDay(), mealType)

    suspend fun addFood(food: FoodLogEntity): Long = dao.insertFood(food)

    suspend fun deleteFood(food: FoodLogEntity) = dao.deleteFood(food)

    suspend fun getFoodsByDateOnce(date: Long): List<FoodLogEntity> =
        dao.getFoodsByDateOnce(date)

    suspend fun getTotalCaloriesByDate(date: LocalDate): Double =
        dao.getTotalCaloriesByDate(date.toEpochDay()) ?: 0.0
}
