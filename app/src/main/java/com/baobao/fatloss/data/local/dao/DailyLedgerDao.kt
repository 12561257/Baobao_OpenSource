package com.baobao.fatloss.data.local.dao

import androidx.room.*
import com.baobao.fatloss.data.local.entity.DailyLedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLedgerDao {
    @Query("SELECT * FROM daily_ledger WHERE date = :date")
    fun getLedger(date: Long): Flow<DailyLedgerEntity?>

    @Query("SELECT * FROM daily_ledger WHERE date = :date")
    suspend fun getLedgerOnce(date: Long): DailyLedgerEntity?

    @Upsert
    suspend fun upsertLedger(ledger: DailyLedgerEntity)

    @Query("SELECT * FROM daily_ledger WHERE date BETWEEN :start AND :end ORDER BY date")
    suspend fun getLedgerRange(start: Long, end: Long): List<DailyLedgerEntity>

    @Query("SELECT * FROM daily_ledger ORDER BY date DESC")
    fun getAllLedgers(): Flow<List<DailyLedgerEntity>>

    @Query("UPDATE daily_ledger SET consumedCalories = consumedCalories + :delta, updatedAt = :now WHERE date = :date")
    suspend fun updateConsumed(date: Long, delta: Double, now: Long = System.currentTimeMillis())

    @Query("UPDATE daily_ledger SET burnedCalories = burnedCalories + :delta, updatedAt = :now WHERE date = :date")
    suspend fun updateBurned(date: Long, delta: Double, now: Long = System.currentTimeMillis())

    @Query("UPDATE daily_ledger SET waterMl = waterMl + :delta, updatedAt = :now WHERE date = :date")
    suspend fun updateWater(date: Long, delta: Int, now: Long = System.currentTimeMillis())
}
