package com.example.data.repository

import android.content.Context
import com.example.utils.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LanguageRepository(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)
    private val _currentLanguage = MutableStateFlow(
        if (prefs.getString("selected_lang", "EN") == "HI") AppLanguage.HINDI else AppLanguage.ENGLISH
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun toggleLanguage() {
        val newLang = if (_currentLanguage.value == AppLanguage.ENGLISH) AppLanguage.HINDI else AppLanguage.ENGLISH
        _currentLanguage.value = newLang
        prefs.edit().putString("selected_lang", if (newLang == AppLanguage.HINDI) "HI" else "EN").apply()
    }

    fun setLanguage(lang: AppLanguage) {
        _currentLanguage.value = lang
        prefs.edit().putString("selected_lang", if (lang == AppLanguage.HINDI) "HI" else "EN").apply()
    }
}
