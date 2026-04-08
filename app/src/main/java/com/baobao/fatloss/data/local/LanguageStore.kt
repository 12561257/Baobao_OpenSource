package com.baobao.fatloss.data.local

import android.content.Context
import android.content.SharedPreferences

class LanguageStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LANGUAGE = "app_language"
        const val LANG_SYSTEM = "system"
        const val LANG_ZH = "zh"
        const val LANG_EN = "en"

        /** 同步读取，可在 attachBaseContext 中调用 */
        fun getLanguageSync(context: Context): String {
            val prefs = context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
            return prefs.getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
        }
    }

    fun getLanguage(): String = prefs.getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM

    fun saveLanguage(language: String) {
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }
}
