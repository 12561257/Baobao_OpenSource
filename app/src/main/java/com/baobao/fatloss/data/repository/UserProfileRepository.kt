package com.baobao.fatloss.data.repository

import com.baobao.fatloss.data.local.dao.UserProfileDao
import com.baobao.fatloss.data.local.entity.UserProfileEntity
import com.baobao.fatloss.data.model.CalorieCalc
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class UserProfileRepository(private val dao: UserProfileDao) {
    fun getProfile(): Flow<UserProfileEntity?> = dao.getProfile()

    suspend fun getProfileOnce(): UserProfileEntity? = dao.getProfileOnce()

    suspend fun hasProfile(): Boolean = dao.hasProfile()

    suspend fun saveProfile(profile: UserProfileEntity) {
        val bmr = CalorieCalc.calculateBMR(profile.currentWeight, profile.heightCm, profile.age, profile.gender)
        val tdee = CalorieCalc.calculateTDEE(bmr, profile.activityLevel)
        val budget = CalorieCalc.dailyBudget(bmr) // BMR - 400
        val updated = profile.copy(
            baseBmr = bmr,
            baseTdee = tdee,
            dailyBudget = budget,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertProfile(updated)
    }

    suspend fun updateWeight(newWeight: Double) {
        val profile = dao.getProfileOnce() ?: return
        val updated = profile.copy(
            currentWeight = newWeight,
            updatedAt = System.currentTimeMillis()
        )
        saveProfile(updated)
    }

    suspend fun updateAiPersona(persona: String) {
        val profile = dao.getProfileOnce() ?: return
        val updated = profile.copy(
            aiPersona = persona,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertProfile(updated)
    }

    suspend fun deleteProfile() {
        dao.deleteProfile()
    }
}
