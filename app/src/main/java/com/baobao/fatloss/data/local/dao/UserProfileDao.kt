package com.baobao.fatloss.data.local.dao

import androidx.room.*
import com.baobao.fatloss.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE userId = 'default_user'")
    fun getProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE userId = 'default_user'")
    suspend fun getProfileOnce(): UserProfileEntity?

    @Upsert
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM user_profile)")
    suspend fun hasProfile(): Boolean

    @Query("DELETE FROM user_profile")
    suspend fun deleteProfile()
}
