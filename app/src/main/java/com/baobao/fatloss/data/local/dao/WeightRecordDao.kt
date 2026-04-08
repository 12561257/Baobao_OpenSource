package com.baobao.fatloss.data.local.dao

import androidx.room.*
import com.baobao.fatloss.data.local.entity.WeightRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightRecordDao {
    @Query("SELECT * FROM weight_record ORDER BY date DESC")
    fun getAllRecords(): Flow<List<WeightRecordEntity>>

    @Query("SELECT * FROM weight_record ORDER BY date ASC")
    fun getAllRecordsAsc(): Flow<List<WeightRecordEntity>>

    @Query("SELECT * FROM weight_record WHERE date BETWEEN :start AND :end ORDER BY date")
    suspend fun getRecordsRange(start: Long, end: Long): List<WeightRecordEntity>

    @Upsert
    suspend fun upsertRecord(record: WeightRecordEntity)

    @Query("SELECT * FROM weight_record ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): WeightRecordEntity?
}
