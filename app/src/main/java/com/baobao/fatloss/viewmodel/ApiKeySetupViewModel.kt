package com.baobao.fatloss.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baobao.fatloss.R
import com.baobao.fatloss.data.local.ApiKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class ApiKeySetupUiState(
    val apiKey: String = "",
    val selectedModelId: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val showHelp: Boolean = false,
    val isTesting: Boolean = false,
    val testSuccess: Boolean? = null,
    val testMessage: String? = null,
    val baseUrl: String = ""
)

class ApiKeySetupViewModel(
    private val apiKeyStore: ApiKeyStore,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApiKeySetupUiState())
    val uiState: StateFlow<ApiKeySetupUiState> = _uiState.asStateFlow()

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    init {
        viewModelScope.launch {
            val existingKey = apiKeyStore.getApiKey()
            val existingModel = apiKeyStore.getModelId()
            val existingUrl = apiKeyStore.getBaseUrl()
            _uiState.update {
                it.copy(
                    apiKey = existingKey ?: "",
                    selectedModelId = existingModel,
                    baseUrl = existingUrl
                )
            }
        }
    }

    fun updateApiKey(key: String) {
        _uiState.update { it.copy(apiKey = key, error = null, testSuccess = null, testMessage = null) }
    }

    fun updateModel(modelId: String) {
        val preset = ApiKeyStore.PRESET_MODELS.find { it.defaultModelId == modelId || it.id == modelId }
        _uiState.update { 
            it.copy(
                selectedModelId = modelId,
                baseUrl = if (preset != null && preset.id != "custom") preset.baseUrl else it.baseUrl,
                testSuccess = null, 
                testMessage = null
            ) 
        }
    }

    fun updateBaseUrl(url: String) {
        _uiState.update { it.copy(baseUrl = url, testSuccess = null, testMessage = null) }
    }

    fun toggleHelp() {
        _uiState.update { it.copy(showHelp = !it.showHelp) }
    }

    fun testConnection() {
        val key = _uiState.value.apiKey.trim()
        val model = _uiState.value.selectedModelId.trim()
        val baseUrl = _uiState.value.baseUrl.trim()

        if (key.isBlank()) {
            _uiState.update { it.copy(error = context.getString(R.string.error_api_key_empty)) }
            return
        }
        if (model.isBlank()) {
            _uiState.update { it.copy(error = context.getString(R.string.error_model_required)) }
            return
        }
        if (baseUrl.isBlank()) {
            _uiState.update { it.copy(error = context.getString(R.string.error_base_url_required_setup)) }
            return
        }

        _uiState.update { it.copy(isTesting = true, testSuccess = null, testMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonMediaType = "application/json; charset=utf-8".toMediaType()
                val body = buildJsonObject {
                    put("model", model)
                    put("messages", kotlinx.serialization.json.buildJsonArray {
                        add(buildJsonObject {
                            put("role", "user")
                            put("content", context.getString(R.string.error_test_reply_ok))
                        })
                    })
                }.toString().toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url("${baseUrl.removeSuffix("/")}/chat/completions")
                    .header("Authorization", "Bearer $key")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val content = try {
                        JSONObject(responseBody)
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                    } catch (_: Exception) { context.getString(R.string.error_analysis_failed) }
                    _uiState.update {
                        it.copy(
                            isTesting = false,
                            testSuccess = true,
                            testMessage = context.getString(R.string.error_test_success, content.take(80))
                        )
                    }
                } else {
                    val errorMsg = try {
                        JSONObject(responseBody).getJSONObject("error").getString("message")
                    } catch (_: Exception) { responseBody.take(200) }
                    _uiState.update {
                        it.copy(
                            isTesting = false,
                            testSuccess = false,
                            testMessage = context.getString(R.string.error_test_http, response.code.toString(), errorMsg)
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        testSuccess = false,
                        testMessage = context.getString(R.string.error_network, e.message)
                    )
                }
            }
        }
    }

    fun save() {
        val state = _uiState.value
        val key = state.apiKey.trim()
        val model = state.selectedModelId.trim()

        if (key.isBlank()) {
            _uiState.update { it.copy(error = context.getString(R.string.error_api_key_empty)) }
            return
        }
        if (model.isBlank()) {
            _uiState.update { it.copy(error = context.getString(R.string.error_model_required)) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                apiKeyStore.saveApiKey(key)
                apiKeyStore.saveModelId(model)
                apiKeyStore.saveBaseUrl(state.baseUrl.trim())
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = context.getString(R.string.error_save_failed, e.message)) }
            }
        }
    }

    class Factory(
        private val apiKeyStore: ApiKeyStore,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ApiKeySetupViewModel(apiKeyStore, context.applicationContext) as T
    }
}
