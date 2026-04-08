package com.baobao.fatloss.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baobao.fatloss.data.local.entity.WeightRecordEntity
import com.baobao.fatloss.data.model.CalorieCalc
import com.baobao.fatloss.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ProgressUiState(
    val currentWeight: Double = 0.0,
    val initialWeight: Double = 0.0,
    val weightLost: Double = 0.0,
    val targetWeight: Double = 0.0,
    val weightToGo: Double = 0.0,
    val bmi: Double = 0.0,
    val avgDailyCalories: Double = 0.0,
    val weightTrend: List<WeightRecordEntity> = emptyList(),
    val weeklyOnTrackDays: Int = 0,
    val weeklyAvgCalories: Double = 0.0,
    val weeklyBurned: Double = 0.0,
    val weeklyWeightChange: Double = 0.0,
    val showWeightDialog: Boolean = false,
    val isLoading: Boolean = true
)

class ProgressViewModel(
    private val weightRecordRepo: WeightRecordRepository,
    private val userProfileRepo: UserProfileRepository,
    private val dailyLedgerRepo: DailyLedgerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // 体重趋势：响应式 Flow，数据变化时自动刷新
        viewModelScope.launch {
            val profile = userProfileRepo.getProfileOnce()
            if (profile == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            weightRecordRepo.getAllRecordsAsc().collectLatest { records ->
                // 在 Flow 内部获取最新记录，确保 currentWeight 始终最新
                val latestRecord = weightRecordRepo.getLatest()
                val currentWeight = latestRecord?.weight ?: profile.currentWeight

                val weekStart = LocalDate.now().minusDays(6)
                val weekLedgers = dailyLedgerRepo.getLedgerRange(weekStart, LocalDate.now())

                // 用最近一周内的体重记录计算周体重变化
                val weekAgoEpoch = weekStart.toEpochDay()
                val weeklyRecords = records.filter { it.date >= weekAgoEpoch }
                val weeklyWeightChange = if (weeklyRecords.size >= 2) {
                    weeklyRecords.last().weight - weeklyRecords.first().weight
                } else {
                    0.0
                }

                _uiState.update { it.copy(
                    currentWeight = currentWeight,
                    initialWeight = profile.initialWeight,
                    weightLost = profile.initialWeight - currentWeight,
                    targetWeight = profile.targetWeight,
                    weightToGo = currentWeight - profile.targetWeight,
                    bmi = CalorieCalc.bmi(currentWeight, profile.heightCm),
                    weightTrend = records,
                    weeklyOnTrackDays = weekLedgers.count { l -> l.consumedCalories <= l.dailyBudget },
                    weeklyAvgCalories = if (weekLedgers.isNotEmpty()) weekLedgers.map { l -> l.consumedCalories }.average() else 0.0,
                    weeklyBurned = weekLedgers.sumOf { l -> l.burnedCalories },
                    weeklyWeightChange = weeklyWeightChange,
                    isLoading = false
                )}
            }
        }

        // 日均摄入（最近30天）
        viewModelScope.launch {
            val end = LocalDate.now()
            val start = end.minusDays(29)
            val ledgers = dailyLedgerRepo.getLedgerRange(start, end)
            val avg = if (ledgers.isNotEmpty()) ledgers.map { it.consumedCalories }.average() else 0.0
            _uiState.update { it.copy(avgDailyCalories = avg) }
        }
    }

    fun showWeightDialog() { _uiState.update { it.copy(showWeightDialog = true) } }
    fun hideWeightDialog() { _uiState.update { it.copy(showWeightDialog = false) } }

    fun recordWeight(weight: Double) {
        viewModelScope.launch {
            weightRecordRepo.addRecord(LocalDate.now(), weight)
            userProfileRepo.updateWeight(weight)
            _uiState.update { it.copy(showWeightDialog = false) }
            // 不需要手动调用 loadData()，因为 getAllRecordsAsc() 是 Flow，
            // 插入新记录后会自动触发 collectLatest 重新发射数据
        }
    }

    class Factory(
        private val weightRecordRepo: WeightRecordRepository,
        private val userProfileRepo: UserProfileRepository,
        private val dailyLedgerRepo: DailyLedgerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProgressViewModel(weightRecordRepo, userProfileRepo, dailyLedgerRepo) as T
    }
}
