package com.baobao.fatloss.data.repository

import com.baobao.fatloss.data.local.dao.DailyLedgerDao
import com.baobao.fatloss.data.local.entity.DailyLedgerEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class DailyLedgerRepository(private val dao: DailyLedgerDao) {
    fun getLedger(date: LocalDate): Flow<DailyLedgerEntity?> =
        dao.getLedger(date.toEpochDay())

    suspend fun getOrCreateToday(budget: Double): DailyLedgerEntity {
        val today = LocalDate.now().toEpochDay()
        val existing = dao.getLedgerOnce(today)
        return if (existing != null) {
            // 如果预算有变化，更新账本
            if (existing.dailyBudget != budget) {
                val updated = existing.copy(dailyBudget = budget, updatedAt = System.currentTimeMillis())
                dao.upsertLedger(updated)
                updated
            } else existing
        } else {
            dao.upsertLedger(DailyLedgerEntity(date = today, dailyBudget = budget)).let {
                dao.getLedgerOnce(today)!!
            }
        }
    }

    /** 更新今日账本的预算 */
    suspend fun updateBudget(date: Long, newBudget: Double) {
        val ledger = dao.getLedgerOnce(date) ?: return
        dao.upsertLedger(ledger.copy(dailyBudget = newBudget, updatedAt = System.currentTimeMillis()))
    }

    suspend fun addToConsumed(date: Long, calories: Double) {
        dao.updateConsumed(date, calories)
    }

    suspend fun subtractConsumed(date: Long, calories: Double) {
        // 使用负数进行原子减法
        dao.updateConsumed(date, -calories)
    }

    suspend fun addBurned(date: Long, calories: Double) {
        dao.updateBurned(date, calories)
    }

    suspend fun updateWaterIntake(date: Long, deltaMl: Int) {
        dao.updateWater(date, deltaMl)
    }

    fun getAllLedgers(): Flow<List<DailyLedgerEntity>> = dao.getAllLedgers()

    suspend fun getLedgerRange(start: LocalDate, end: LocalDate): List<DailyLedgerEntity> =
        dao.getLedgerRange(start.toEpochDay(), end.toEpochDay())
}
