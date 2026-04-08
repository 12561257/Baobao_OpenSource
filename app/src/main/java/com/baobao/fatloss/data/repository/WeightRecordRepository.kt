package com.baobao.fatloss.data.repository

import com.baobao.fatloss.data.local.dao.WeightRecordDao
import com.baobao.fatloss.data.local.entity.WeightRecordEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class WeightRecordRepository(private val dao: WeightRecordDao) {
    fun getAllRecordsAsc(): Flow<List<WeightRecordEntity>> = dao.getAllRecordsAsc()

    suspend fun addRecord(date: LocalDate, weight: Double) {
        dao.upsertRecord(WeightRecordEntity(date = date.toEpochDay(), weight = weight))
    }

    suspend fun getLatest(): WeightRecordEntity? = dao.getLatest()

    suspend fun getRecordsRange(start: LocalDate, end: LocalDate): List<WeightRecordEntity> =
        dao.getRecordsRange(start.toEpochDay(), end.toEpochDay())
}
