package com.pocketpal.ui.screens.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.data.repository.VoiceEngineRepository
import kotlinx.coroutines.launch

class VoicePacksViewModel(private val repository: VoiceEngineRepository) : ViewModel() {

    val voicePacks = repository.voicePacks

    fun downloadPack(id: String) {
        viewModelScope.launch {
            repository.downloadVoicePack(id) {}
        }
    }

    fun toggleLoad(id: String) {
        repository.toggleLoadVoicePack(id)
    }
}
