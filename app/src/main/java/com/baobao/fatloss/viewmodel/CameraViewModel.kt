package com.baobao.fatloss.viewmodel

import androidx.annotation.StringRes
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

/**
 * UI 层文本封装：支持动态资源和纯文本
 */
sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    data class StringResource(@StringRes val resId: Int, val args: Array<Any> = emptyArray()) : UiText()
}

data class ParsedFood(
    val name: String,
    val weight: String = "",
    val calories: Double = 0.0,
    val carbs: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0
)

data class CameraUiState(
    val inputText: String = "",
    val mealType: String = "lunch",
    val parsedFoods: List<ParsedFood> = emptyList(),
    val totalCalories: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalFat: Double = 0.0,
    val aiComment: String = "",
    val isAnalyzing: Boolean = false,
    val isConfirmed: Boolean = false,
    val error: UiText? = null,
    val imageBase64: String? = null,
    val hasImage: Boolean = false
)

class CameraViewModel(
    private val foodLogRepo: FoodLogRepository,
    private val dailyLedgerRepo: DailyLedgerRepository,
    private val aiRepo: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun setImageBase64(base64: String) {
        _uiState.update { it.copy(imageBase64 = base64, hasImage = true) }
        analyzeFoodImage()
    }

    private fun analyzeFoodImage() {
        val base64 = _uiState.value.imageBase64 ?: return
        _uiState.update { it.copy(isAnalyzing = true, error = null) }

        viewModelScope.launch {
            try {
                val response = aiRepo.analyzeFoodImage(base64)
                val result = parseFoodResponse(response)
                _uiState.update { it.copy(
                    parsedFoods = result.foods,
                    totalCalories = result.totalCal,
                    totalCarbs = result.foods.sumOf { f -> f.carbs },
                    totalProtein = result.foods.sumOf { f -> f.protein },
                    totalFat = result.foods.sumOf { f -> f.fat },
                    aiComment = result.comment,
                    isAnalyzing = false
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(isAnalyzing = false, error = UiText.StringResource(R.string.error_parse_failed, arrayOf(e.message ?: ""))) }
            }
        }
    }

    fun selectMealType(type: String) {
        _uiState.update { it.copy(mealType = type) }
    }

    fun analyzeFood() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        _uiState.update { it.copy(isAnalyzing = true, error = null) }

        viewModelScope.launch {
            try {
                val response = aiRepo.analyzeFood(text)
                val result = parseFoodResponse(response)
                _uiState.update { it.copy(
                    parsedFoods = result.foods,
                    totalCalories = result.totalCal,
                    totalCarbs = result.foods.sumOf { f -> f.carbs },
                    totalProtein = result.foods.sumOf { f -> f.protein },
                    totalFat = result.foods.sumOf { f -> f.fat },
                    aiComment = result.comment,
                    isAnalyzing = false
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(isAnalyzing = false, error = UiText.StringResource(R.string.error_parse_failed, arrayOf(e.message ?: ""))) }
            }
        }
    }

    fun confirmRecord() {
        val state = _uiState.value
        if (state.parsedFoods.isEmpty()) return

        viewModelScope.launch {
            val date = LocalDate.now().toEpochDay()
            val mealType = state.mealType
            for (food in state.parsedFoods) {
                foodLogRepo.addFood(FoodLogEntity(
                    date = date,
                    mealType = mealType,
                    foodDescription = "${food.name} ${food.weight}".trim(),
                    estimatedCal = food.calories,
                    carbsG = food.carbs,
                    proteinG = food.protein,
                    fatG = food.fat,
                    aiComment = state.aiComment
                ))
            }
            dailyLedgerRepo.addToConsumed(date, state.totalCalories)
            _uiState.update { it.copy(isConfirmed = true) }
        }
    }

    fun reset() {
        _uiState.update { CameraUiState() }
    }

    private data class FoodParseResult(val foods: List<ParsedFood>, val totalCal: Double, val comment: String)

    private fun parseFoodResponse(response: String): FoodParseResult {
        return try {
            val jsonRegex = Regex("""\{.*\}""", RegexOption.DOT_MATCHES_ALL)
            val jsonStr = jsonRegex.find(response)?.value ?: "{}"
            val json = kotlinx.serialization.json.Json.parseToJsonElement(jsonStr).jsonObject

            val foodsArray = json["foods"]?.jsonArray ?: emptyList()
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

    class Factory(
        private val foodLogRepo: FoodLogRepository,
        private val dailyLedgerRepo: DailyLedgerRepository,
        private val aiRepo: AiRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CameraViewModel(foodLogRepo, dailyLedgerRepo, aiRepo) as T
    }
}
