package com.pocketpal.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.pocketpal.data.model.ChatSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel : ViewModel() {

    private val _settings = MutableStateFlow(ChatSettings())
    val settings = _settings.asStateFlow()

    fun updateLanguage(lang: String) {
        _settings.update { it.copy(language = lang) }
    }

    fun updateTemperature(temp: Float) {
        _settings.update { it.copy(temperature = temp) }
    }

    fun updateSystemPrompt(prompt: String) {
        _settings.update { it.copy(systemPrompt = prompt) }
    }

    fun updateTtsEnabled(enabled: Boolean) {
        _settings.update { it.copy(ttsEnabled = enabled) }
    }

    fun updateOfflineOnlyMode(enabled: Boolean) {
        _settings.update { it.copy(isOfflineOnlyMode = enabled) }
    }
}
