package com.baobao.fatloss.data.local.dao

import androidx.room.*
import com.baobao.fatloss.data.local.entity.FoodLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {
    @Query("SELECT * FROM food_log WHERE date = :date ORDER BY mealType, loggedAt")
    fun getFoodsByDate(date: Long): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_log WHERE date = :date AND mealType = :mealType ORDER BY loggedAt")
    fun getFoodsByDateAndMeal(date: Long, mealType: String): Flow<List<FoodLogEntity>>

    @Insert
    suspend fun insertFood(food: FoodLogEntity): Long

    @Delete
    suspend fun deleteFood(food: FoodLogEntity)

    @Query("SELECT SUM(estimatedCal) FROM food_log WHERE date = :date")
    suspend fun getTotalCaloriesByDate(date: Long): Double?

    @Query("SELECT SUM(carbsG) FROM food_log WHERE date = :date")
    suspend fun getTotalCarbsByDate(date: Long): Double?

    @Query("SELECT SUM(proteinG) FROM food_log WHERE date = :date")
    suspend fun getTotalProteinByDate(date: Long): Double?

    @Query("SELECT SUM(fatG) FROM food_log WHERE date = :date")
    suspend fun getTotalFatByDate(date: Long): Double?

    @Query("SELECT * FROM food_log WHERE date = :date ORDER BY mealType, loggedAt")
    suspend fun getFoodsByDateOnce(date: Long): List<FoodLogEntity>
}
