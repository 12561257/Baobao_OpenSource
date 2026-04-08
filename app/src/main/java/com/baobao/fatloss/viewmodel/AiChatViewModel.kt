package com.baobao.fatloss.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baobao.fatloss.data.local.entity.ChatMessageEntity
import com.baobao.fatloss.data.local.entity.FoodLogEntity
import com.baobao.fatloss.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 结构化的操作反馈数据，UI 层负责用 stringResource() 解析
 */
sealed class ActionFeedbackData {
    data class FoodRecorded(
        val mealType: String,  // "breakfast" / "lunch" / "dinner" / "snack"
        val foodNames: List<String>,
        val totalCalories: Int
    ) : ActionFeedbackData()

    data class ExerciseRecorded(
        val exerciseName: String,
        val caloriesBurned: Int
    ) : ActionFeedbackData()

    data class FoodDeleted(
        val foodNames: List<String>,
        val totalCalories: Int
    ) : ActionFeedbackData()
}

data class AiChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionFeedbackItems: List<ActionFeedbackData> = emptyList(),
    val selectedImagePath: String? = null
)

class AiChatViewModel(
    private val aiRepo: AiRepository,
    private val foodLogRepo: FoodLogRepository,
    private val dailyLedgerRepo: DailyLedgerRepository,
    private val userProfileRepo: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            aiRepo.getAllMessages().collect { messages: List<ChatMessageEntity> ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun updateSelectedImage(path: String?) {
        _uiState.update { it.copy(selectedImagePath = path) }
    }

    fun clearActionFeedback() {
        _uiState.update { it.copy(actionFeedbackItems = emptyList()) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        val imagePath = _uiState.value.selectedImagePath
        if (text.isEmpty() && imagePath == null) return

        _uiState.update { it.copy(inputText = "", selectedImagePath = null, isLoading = true, error = null, actionFeedbackItems = emptyList()) }

        viewModelScope.launch {
            try {
                val (aiReply, actions) = aiRepo.sendMessageAndParseActions(text, imagePath)

                // 处理解析出的动作
                val feedbackItems = mutableListOf<ActionFeedbackData>()
                val today = LocalDate.now().toEpochDay()

                // 1. 确保今日账本已创建（因为原子更新依赖于已有的行）
                val profile = userProfileRepo.getProfileOnce()
                dailyLedgerRepo.getOrCreateToday(profile?.dailyBudget ?: 1850.0)

                // 2. 记录食物
                if (actions.foodLogs.isNotEmpty()) {
                    val recordedFoods = mutableListOf<String>()
                    var totalCalories = 0.0
                    val firstStandardMeal = FoodLogEntity.mapToStandardMealType(actions.foodLogs.first().meal)

                    for (food in actions.foodLogs) {
                        val standardMeal = FoodLogEntity.mapToStandardMealType(food.meal)
                        val foodEntity = FoodLogEntity(
                            date = today,
                            mealType = standardMeal,
                            foodDescription = food.name,
                            estimatedCal = food.calories,
                            carbsG = food.carbs,
                            proteinG = food.protein,
                            fatG = food.fat,
                            inputMethod = "ai_chat"
                        )
                        foodLogRepo.addFood(foodEntity)
                        totalCalories += food.calories
                        recordedFoods.add(food.name)
                    }

                    // 更新每日账本 - 增加已摄入热量
                    dailyLedgerRepo.addToConsumed(today, totalCalories)

                    feedbackItems.add(ActionFeedbackData.FoodRecorded(
                        mealType = firstStandardMeal,
                        foodNames = recordedFoods,
                        totalCalories = totalCalories.toInt()
                    ))
                }

                // 记录运动
                if (actions.exercise != null) {
                    val ex = actions.exercise

                    // 更新每日账本 - 增加运动消耗热量
                    dailyLedgerRepo.addBurned(today, ex.caloriesBurned)

                    feedbackItems.add(ActionFeedbackData.ExerciseRecorded(
                        exerciseName = ex.name,
                        caloriesBurned = ex.caloriesBurned.toInt()
                    ))
                }

                // 删除食物记录
                if (actions.deleteFoods.isNotEmpty()) {
                    val deletedNames = mutableListOf<String>()
                    var totalRemovedCal = 0.0

                    for (deleteItem in actions.deleteFoods) {
                        try {
                            val allFoods = foodLogRepo.getFoodsByDateOnce(today)
                            val standardMealToRemove = FoodLogEntity.mapToStandardMealType(deleteItem.meal)
                            // 优先精确匹配，再模糊匹配，避免短词误匹配多条
                            val toDelete = allFoods
                                .filter { FoodLogEntity.mapToStandardMealType(it.mealType) == standardMealToRemove }
                                .let { candidates ->
                                    // 1. 精确匹配
                                    candidates.find {
                                        it.foodDescription.equals(deleteItem.foodName, ignoreCase = true)
                                    }
                                    // 2. 记录名包含删除关键词（关键词至少2个字，避免单字误匹配）
                                    ?: candidates.find {
                                        deleteItem.foodName.length >= 2 &&
                                            it.foodDescription.contains(deleteItem.foodName, ignoreCase = true)
                                    }
                                    // 3. 删除关键词包含记录名（记录名至少2个字）
                                    ?: candidates.find {
                                        it.foodDescription.length >= 2 &&
                                            deleteItem.foodName.contains(it.foodDescription, ignoreCase = true)
                                    }
                                }
                            if (toDelete != null) {
                                foodLogRepo.deleteFood(toDelete)
                                totalRemovedCal += toDelete.estimatedCal
                                deletedNames.add(toDelete.foodDescription)
                            }
                        } catch (_: Exception) {}
                    }

                    if (deletedNames.isNotEmpty()) {
                        // 更新每日账本 - 减少已摄入热量
                        dailyLedgerRepo.addToConsumed(today, -totalRemovedCal)
                        feedbackItems.add(ActionFeedbackData.FoodDeleted(
                            foodNames = deletedNames,
                            totalCalories = totalRemovedCal.toInt()
                        ))
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        actionFeedbackItems = feedbackItems
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch { aiRepo.clearAll() }
    }

    class Factory(
        private val aiRepo: AiRepository,
        private val foodLogRepo: FoodLogRepository,
        private val dailyLedgerRepo: DailyLedgerRepository,
        private val userProfileRepo: UserProfileRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AiChatViewModel(aiRepo, foodLogRepo, dailyLedgerRepo, userProfileRepo) as T
    }
}
