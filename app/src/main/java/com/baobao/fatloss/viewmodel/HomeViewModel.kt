package com.baobao.fatloss.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baobao.fatloss.R
import com.baobao.fatloss.data.local.entity.DailyLedgerEntity
import com.baobao.fatloss.data.local.entity.UserProfileEntity
import com.baobao.fatloss.data.model.CalorieCalc
import com.baobao.fatloss.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val userName: String = "",
    val dailyBudget: Double = 0.0,
    val consumedCalories: Double = 0.0,
    val burnedCalories: Double = 0.0,
    val remainingCalories: Double = 0.0,
    val progressPct: Float = 0f,
    val carbsConsumed: Double = 0.0,
    val carbsTarget: Double = 0.0,
    val proteinConsumed: Double = 0.0,
    val proteinTarget: Double = 0.0,
    val fatConsumed: Double = 0.0,
    val fatTarget: Double = 0.0,
    val waterMl: Int = 0,
    val waterTarget: Int = 2000,
    val weeklyWeightChange: Double = 0.0,
    val weeklyAvgCalories: Double = 0.0,
    val weeklyAdherenceRate: Int = 0,
    val aiInsight: String = "",
    val isLoading: Boolean = true,
    val hasProfile: Boolean = false
)

class HomeViewModel(
    private val userProfileRepo: UserProfileRepository,
    private val dailyLedgerRepo: DailyLedgerRepository,
    private val foodLogRepo: FoodLogRepository,
    private val weightRecordRepo: WeightRecordRepository,
    private val aiRepo: AiRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(
        aiInsight = context.getString(R.string.home_no_record_insight)
    ))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // 观察 profile 变化（首次引导保存后会自动触发）
        viewModelScope.launch {
            userProfileRepo.getProfile().collectLatest { profile ->
                if (profile != null) {
                    _uiState.update { it.copy(hasProfile = true, userName = profile.name) }
                    observeLedgerAndFood(profile)
                }
            }
        }
        // 自动生成昨日每日笔记（有食物记录时才触发，幂等）
        viewModelScope.launch {
            try {
                val yesterday = LocalDate.now().minusDays(1).toEpochDay()
                val foods = foodLogRepo.getFoodsByDateOnce(yesterday)
                if (foods.isNotEmpty()) {
                    aiRepo.generateDailyNote()
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * 观察今日账本 + 食物记录，数据变化时自动刷新仪表盘
     */
    private fun observeLedgerAndFood(profile: UserProfileEntity) {
        val today = LocalDate.now()
        // 确保今日账本存在，且同步最新预算
        viewModelScope.launch {
            dailyLedgerRepo.getOrCreateToday(profile.dailyBudget)
            // 即使账本已存在，也要更新预算值（profile可能变了）
            dailyLedgerRepo.updateBudget(today.toEpochDay(), profile.dailyBudget)
        }
        // 观察账本变化
        viewModelScope.launch {
            dailyLedgerRepo.getLedger(today).collectLatest { ledger ->
                if (ledger != null) {
                    val macros = CalorieCalc.macroTargets(ledger.dailyBudget)
                    _uiState.update { it.copy(
                        dailyBudget = ledger.dailyBudget,
                        consumedCalories = ledger.consumedCalories,
                        burnedCalories = ledger.burnedCalories,
                        waterMl = ledger.waterMl,
                        remainingCalories = ledger.netRemaining,
                        progressPct = if (ledger.dailyBudget > 0) (ledger.consumedCalories / ledger.dailyBudget).toFloat().coerceIn(0f, 1f) else 0f,
                        carbsTarget = macros.carbsG,
                        proteinTarget = macros.proteinG,
                        fatTarget = macros.fatG,
                        isLoading = false
                    )}
                }
            }
        }
        // 观察食物记录变化 → 更新营养素
        viewModelScope.launch {
            foodLogRepo.getFoodsByDate(today).collectLatest { foods ->
                val carbs = foods.sumOf { f -> f.carbsG }
                val protein = foods.sumOf { f -> f.proteinG }
                val fat = foods.sumOf { f -> f.fatG }
                _uiState.update { it.copy(
                    carbsConsumed = carbs,
                    proteinConsumed = protein,
                    fatConsumed = fat
                )}
            }
        }
        // 加载周报数据
        viewModelScope.launch {
            loadWeeklyData(profile)
        }
    }

    private suspend fun loadWeeklyData(profile: UserProfileEntity) {
        val today = LocalDate.now()
        val weekStart = today.minusDays(6)
        val weekLedgers = dailyLedgerRepo.getLedgerRange(weekStart, today)
        val weeklyAvg = if (weekLedgers.isNotEmpty()) weekLedgers.map { it.consumedCalories }.average() else 0.0
        val onTrackDays = weekLedgers.count { it.consumedCalories <= it.dailyBudget }
        val adherenceRate = if (weekLedgers.isNotEmpty()) (onTrackDays * 100 / weekLedgers.size) else 0

        val records = weightRecordRepo.getRecordsRange(weekStart, today)
        val weeklyWeightChange = if (records.size >= 2) {
            records.first().weight - records.last().weight
        } else 0.0

        _uiState.update { it.copy(
            weeklyWeightChange = weeklyWeightChange,
            weeklyAvgCalories = weeklyAvg,
            weeklyAdherenceRate = adherenceRate
        )}
    }

    fun addWater() {
        viewModelScope.launch {
            dailyLedgerRepo.updateWaterIntake(LocalDate.now().toEpochDay(), 200)
        }
    }

    fun subtractWater() {
        viewModelScope.launch {
            dailyLedgerRepo.updateWaterIntake(LocalDate.now().toEpochDay(), -200)
        }
    }

    /** 首次引导保存 */
    fun saveInitialProfile(
        name: String, heightCm: Int, weight: Double, targetWeight: Double, age: Int, activityLevel: Int = 2, gender: Int = 1
    ) {
        viewModelScope.launch {
            val profile = UserProfileEntity(
                name = name,
                heightCm = heightCm,
                initialWeight = weight,
                currentWeight = weight,
                targetWeight = targetWeight,
                age = age,
                gender = gender,
                activityLevel = activityLevel,
                targetDate = LocalDate.now().plusMonths(3).toEpochDay()
            )
            userProfileRepo.saveProfile(profile)
            weightRecordRepo.addRecord(LocalDate.now(), weight)
            // profile 变化会自动触发 Flow，无需手动 loadDashboard
        }
    }

    fun refresh() {
        // Flow 自动刷新，此方法保留兼容性
    }

    class Factory(
        private val userProfileRepo: UserProfileRepository,
        private val dailyLedgerRepo: DailyLedgerRepository,
        private val foodLogRepo: FoodLogRepository,
        private val weightRecordRepo: WeightRecordRepository,
        private val aiRepo: AiRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(userProfileRepo, dailyLedgerRepo, foodLogRepo, weightRecordRepo, aiRepo, context.applicationContext) as T
    }
}
