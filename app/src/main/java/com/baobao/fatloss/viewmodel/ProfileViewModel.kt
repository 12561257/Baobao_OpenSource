package com.baobao.fatloss.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baobao.fatloss.data.local.ApiKeyStore
import com.baobao.fatloss.data.local.entity.UserProfileEntity
import com.baobao.fatloss.data.model.CalorieCalc
import com.baobao.fatloss.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ProfileUiState(
    val profile: UserProfileEntity? = null,
    val isEditing: Boolean = false,
    val editName: String = "",
    val editHeightCm: String = "",
    val editCurrentWeight: String = "",
    val editTargetWeight: String = "",
    val editAge: String = "",
    val editGender: Int = 1,
    val editActivityLevel: Int = 2,
    val bmi: Double = 0.0,
    val dailyBudget: Double = 0.0,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val daysStreak: Int = 0,
    val totalWeightLost: Double = 0.0,
    val maskedApiKey: String = ""
)

class ProfileViewModel(
    private val userProfileRepo: UserProfileRepository,
    private val weightRecordRepo: WeightRecordRepository,
    private val apiKeyStore: ApiKeyStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userProfileRepo.getProfile().collectLatest { profile ->
                if (profile != null) {
                    _uiState.update { it.copy(
                        profile = profile,
                        bmi = CalorieCalc.bmi(profile.currentWeight, profile.heightCm),
                        dailyBudget = profile.dailyBudget,
                        totalWeightLost = profile.initialWeight - profile.currentWeight,
                        isLoading = false
                    )}
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
        loadMaskedApiKey()
    }

    private fun loadMaskedApiKey() {
        viewModelScope.launch {
            val masked = apiKeyStore.getMaskedKey()
            _uiState.update { it.copy(maskedApiKey = masked) }
        }
    }

    fun startEditing() {
        val p = _uiState.value.profile ?: UserProfileEntity()
        _uiState.update { it.copy(
            isEditing = true,
            editName = p.name,
            editHeightCm = p.heightCm.toString(),
            editCurrentWeight = p.currentWeight.toString(),
            editTargetWeight = p.targetWeight.toString(),
            editAge = p.age.toString(),
            editGender = p.gender,
            editActivityLevel = p.activityLevel
        )}
    }

    fun cancelEditing() { _uiState.update { it.copy(isEditing = false) } }

    fun updateEditName(v: String) { _uiState.update { it.copy(editName = v) } }
    fun updateEditHeight(v: String) { _uiState.update { it.copy(editHeightCm = v) } }
    fun updateEditWeight(v: String) { _uiState.update { it.copy(editCurrentWeight = v) } }
    fun updateEditTarget(v: String) { _uiState.update { it.copy(editTargetWeight = v) } }
    fun updateEditAge(v: String) { _uiState.update { it.copy(editAge = v) } }
    fun updateEditActivity(v: Int) { _uiState.update { it.copy(editActivityLevel = v) } }
    fun updateEditGender(v: Int) { _uiState.update { it.copy(editGender = v) } }

    fun saveProfile() {
        val state = _uiState.value
        val profile = state.profile ?: UserProfileEntity()
        viewModelScope.launch {
            val updated = profile.copy(
                name = state.editName,
                heightCm = state.editHeightCm.toIntOrNull() ?: profile.heightCm,
                currentWeight = state.editCurrentWeight.toDoubleOrNull() ?: profile.currentWeight,
                targetWeight = state.editTargetWeight.toDoubleOrNull() ?: profile.targetWeight,
                age = state.editAge.toIntOrNull() ?: profile.age,
                gender = state.editGender,
                activityLevel = state.editActivityLevel,
                targetDate = LocalDate.now().plusMonths(3).toEpochDay()
            )
            userProfileRepo.saveProfile(updated)
            _uiState.update { it.copy(isEditing = false, isSaved = true) }
        }
    }

    fun updateAiPersona(persona: String) {
        viewModelScope.launch {
            userProfileRepo.updateAiPersona(persona)
        }
    }

    fun resetProfile() {
        viewModelScope.launch {
            userProfileRepo.deleteProfile()
            _uiState.update { ProfileUiState() }
        }
    }

    /** 首次引导保存 */
    fun saveInitialProfile(
        name: String, heightCm: Int, weight: Double, targetWeight: Double, age: Int, activityLevel: Int, gender: Int
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
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    class Factory(
        private val userProfileRepo: UserProfileRepository,
        private val weightRecordRepo: WeightRecordRepository,
        private val apiKeyStore: ApiKeyStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProfileViewModel(userProfileRepo, weightRecordRepo, apiKeyStore) as T
    }
}
