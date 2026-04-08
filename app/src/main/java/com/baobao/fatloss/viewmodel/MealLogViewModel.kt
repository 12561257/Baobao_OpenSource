package com.baobao.fatloss.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baobao.fatloss.R
import com.baobao.fatloss.data.local.entity.FoodLogEntity
import com.baobao.fatloss.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class MealLogUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val dateLabel: String = "",
    val breakfastFoods: List<FoodLogEntity> = emptyList(),
    val lunchFoods: List<FoodLogEntity> = emptyList(),
    val dinnerFoods: List<FoodLogEntity> = emptyList(),
    val snackFoods: List<FoodLogEntity> = emptyList(),
    val dailyBudget: Double = 0.0,
    val totalConsumed: Double = 0.0,
    val netRemaining: Double = 0.0,
    val showAddDialog: Boolean = false,
    val addMealType: String = "lunch",
    val isLoading: Boolean = true,
    // AI 分析对话框状态
    val addFoodInput: String = "",
    val isAnalyzing: Boolean = false,
    val analyzedFoods: List<ParsedFood> = emptyList(),
    val analysisTotalCal: Double = 0.0,
    val analysisComment: String = "",
    val analysisError: UiText? = null,
    // 图片分析
    val dialogImageBase64: String? = null,
    val analysisMode: String = "text" // "text" or "photo"
)

class MealLogViewModel(
    private val foodLogRepo: FoodLogRepository,
    private val dailyLedgerRepo: DailyLedgerRepository,
    private val userProfileRepo: UserProfileRepository,
    private val aiRepo: AiRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealLogUiState())
    val uiState: StateFlow<MealLogUiState> = _uiState.asStateFlow()

    init {
        updateDateLabel()
        loadDay()
    }

    private fun updateDateLabel() {
        val formatter = DateTimeFormatter.ofPattern(context.getString(R.string.date_format_pattern))
        _uiState.update { it.copy(dateLabel = it.selectedDate.format(formatter)) }
    }

    private fun loadDay() {
        viewModelScope.launch {
            val profile = userProfileRepo.getProfileOnce()
            val budget = profile?.dailyBudget ?: 0.0
            _uiState.update { it.copy(dailyBudget = budget) }

            val date = _uiState.value.selectedDate
            foodLogRepo.getFoodsByDate(date).collectLatest { foods ->
                val totalConsumed = foods.sumOf { f -> f.estimatedCal }
                _uiState.update { state ->
                    state.copy(
                        breakfastFoods = foods.filter { FoodLogEntity.mapToStandardMealType(it.mealType) == "breakfast" },
                        lunchFoods = foods.filter { FoodLogEntity.mapToStandardMealType(it.mealType) == "lunch" },
                        dinnerFoods = foods.filter { FoodLogEntity.mapToStandardMealType(it.mealType) == "dinner" },
                        snackFoods = foods.filter { FoodLogEntity.mapToStandardMealType(it.mealType) == "snack" },
                        totalConsumed = totalConsumed,
                        netRemaining = state.dailyBudget - totalConsumed,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun previousDay() {
        _uiState.update { it.copy(selectedDate = it.selectedDate.minusDays(1), isLoading = true) }
        updateDateLabel()
        loadDay()
    }

    fun nextDay() {
        _uiState.update { it.copy(selectedDate = it.selectedDate.plusDays(1), isLoading = true) }
        updateDateLabel()
        loadDay()
    }

    fun showAddMealDialog(mealType: String) {
        _uiState.update { it.copy(showAddDialog = true, addMealType = mealType) }
    }

    fun hideAddDialog() {
        _uiState.update { it.copy(
            showAddDialog = false,
            addFoodInput = "",
            analyzedFoods = emptyList(),
            analysisTotalCal = 0.0,
            analysisComment = "",
            analysisError = null,
            isAnalyzing = false,
            dialogImageBase64 = null,
            analysisMode = "text"
        )}
    }

    fun setAnalysisMode(mode: String) {
        _uiState.update { it.copy(analysisMode = mode, analyzedFoods = emptyList(), dialogImageBase64 = null) }
    }

    fun setDialogImageBase64(base64: String) {
        _uiState.update { it.copy(dialogImageBase64 = base64, isAnalyzing = true, analysisError = null) }
        analyzeDialogImage()
    }

    private fun analyzeDialogImage() {
        val base64 = _uiState.value.dialogImageBase64 ?: return
        viewModelScope.launch {
            try {
                val response = aiRepo.analyzeFoodImage(base64)
                val result = parseFoodResponse(response)
                _uiState.update { it.copy(
                    analyzedFoods = result.foods,
                    analysisTotalCal = result.totalCal,
                    analysisComment = result.comment,
                    isAnalyzing = false
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(isAnalyzing = false, analysisError = UiText.StringResource(R.string.error_parse_failed, arrayOf(e.message ?: ""))) }
            }
        }
    }

    fun updateAddFoodInput(text: String) {
        _uiState.update { it.copy(addFoodInput = text) }
    }

    fun analyzeFoodInput() {
        val text = _uiState.value.addFoodInput.trim()
        if (text.isEmpty()) return
        _uiState.update { it.copy(isAnalyzing = true, analysisError = null) }

        viewModelScope.launch {
            try {
                val response = aiRepo.analyzeFood(text)
                val result = parseFoodResponse(response)
                _uiState.update { it.copy(
                    analyzedFoods = result.foods,
                    analysisTotalCal = result.totalCal,
                    analysisComment = result.comment,
                    isAnalyzing = false
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(isAnalyzing = false, analysisError = UiText.DynamicString(e.message ?: "")) }
            }
        }
    }

    fun confirmAnalyzedFoods() {
        val state = _uiState.value
        if (state.analyzedFoods.isEmpty()) return

        viewModelScope.launch {
            val date = state.selectedDate.toEpochDay()
            val mealType = state.addMealType
            for (food in state.analyzedFoods) {
                foodLogRepo.addFood(FoodLogEntity(
                    date = date,
                    mealType = mealType,
                    foodDescription = "${food.name} ${food.weight}".trim(),
                    estimatedCal = food.calories,
                    carbsG = food.carbs,
                    proteinG = food.protein,
                    fatG = food.fat
                ))
            }
            dailyLedgerRepo.addToConsumed(date, state.analysisTotalCal)
            hideAddDialog()
        }
    }

    private data class FoodParseResult(val foods: List<ParsedFood>, val totalCal: Double, val comment: String)

    private fun parseFoodResponse(response: String): FoodParseResult {
        return try {
            val jsonRegex = Regex("""\{.*\}""", RegexOption.DOT_MATCHES_ALL)
            val jsonStr = jsonRegex.find(response)?.value ?: "{}"
            val json = kotlinx.serialization.json.Json.parseToJsonElement(jsonStr).jsonObject

            val foodsArray = json["foods"]?.jsonArray ?: emptyList<kotlinx.serialization.json.JsonElement>()
            val foods = foodsArray.mapNotNull { element ->
                val obj = element.jsonObject
                ParsedFood(
                    name = obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    weight = obj["weight"]?.jsonPrimitive?.content ?: "",
                    calories = obj["calories"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    carbs = obj["carbs"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    protein = obj["protein"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    fat = obj["fat"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                )
            }
            val totalCal = json["total_cal"]?.jsonPrimitive?.doubleOrNull ?: foods.sumOf { food -> food.calories }
            val comment = json["ai_comment"]?.jsonPrimitive?.content ?: ""
            FoodParseResult(foods, totalCal, comment)
        } catch (e: Exception) {
            FoodParseResult(emptyList(), 0.0, "")
        }
    }

    fun deleteFood(food: FoodLogEntity) {
        viewModelScope.launch {
            foodLogRepo.deleteFood(food)
            dailyLedgerRepo.subtractConsumed(food.date, food.estimatedCal)
        }
    }

    class Factory(
        private val foodLogRepo: FoodLogRepository,
        private val dailyLedgerRepo: DailyLedgerRepository,
        private val userProfileRepo: UserProfileRepository,
        private val aiRepo: AiRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MealLogViewModel(foodLogRepo, dailyLedgerRepo, userProfileRepo, aiRepo, context.applicationContext) as T
    }
}
