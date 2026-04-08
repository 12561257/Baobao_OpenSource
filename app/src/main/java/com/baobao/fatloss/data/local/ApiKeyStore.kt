package com.baobao.fatloss.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class ApiKeyStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "api_key_store",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL_ID = "model_id"
        private const val KEY_BASE_URL = "base_url"

        data class ProviderPreset(
            val id: String,
            val displayName: String,
            val baseUrl: String,
            val defaultModelId: String
        )

        val PRESET_MODELS = listOf(
            ProviderPreset("doubao_lite", "豆包 Seed Lite", "https://ark.cn-beijing.volces.com/api/v3", "doubao-seed-2-0-lite-260215"),
            ProviderPreset("custom", "自定义", "", "")
        )
    }

    suspend fun getApiKey(): String? = prefs.getString(KEY_API_KEY, null)

    suspend fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    suspend fun getModelId(): String {
        return prefs.getString(KEY_MODEL_ID, null) ?: ""
    }

    suspend fun saveModelId(model: String) {
        prefs.edit().putString(KEY_MODEL_ID, model.trim()).apply()
    }

    suspend fun getBaseUrl(): String {
        return prefs.getString(KEY_BASE_URL, null) ?: ""
    }

    suspend fun saveBaseUrl(url: String) {
        prefs.edit().putString(KEY_BASE_URL, url.trim()).apply()
    }

    suspend fun isConfigured(): Boolean {
        return !prefs.getString(KEY_API_KEY, null).isNullOrBlank()
    }

    suspend fun getMaskedKey(): String {
        val key = getApiKey() ?: return ""
        return when {
            key.length <= 8 -> "****"
            else -> "${key.take(4)}${"*".repeat(key.length - 8)}${key.takeLast(4)}"
        }
    }

    suspend fun clearAll() {
        prefs.edit().clear().apply()
    }
}
