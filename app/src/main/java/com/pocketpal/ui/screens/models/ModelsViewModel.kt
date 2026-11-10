package com.pocketpal.ui.screens.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.data.model.AiModel
import com.pocketpal.data.repository.ModelRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ModelsViewModel(private val modelRepository: ModelRepository) : ViewModel() {

    val models = modelRepository.models
    val activeModelId = modelRepository.activeModelId

    private val _hfSearchQuery = MutableStateFlow("")
    val hfSearchQuery = _hfSearchQuery.asStateFlow()

    fun setHfSearchQuery(query: String) {
        _hfSearchQuery.value = query
    }

    fun selectActiveModel(modelId: String) {
        modelRepository.selectActiveModel(modelId)
    }

    fun startDownload(modelId: String) {
        viewModelScope.launch {
            for (p in 1..10) {
                delay(200)
                modelRepository.updateDownloadProgress(modelId, p / 10f)
            }
        }
    }

    fun addHuggingFaceModel(repoId: String) {
        if (repoId.isBlank()) return
        val name = repoId.substringAfterLast("/")
        val customModel = AiModel(
            id = "hf_${repoId.hashCode()}",
            name = "$name GGUF",
            provider = "Hugging Face",
            sizeBytes = 2_100_000_000L,
            isDownloaded = false,
            isLoaded = false,
            quantization = "Q4_K_M",
            description = "Custom GGUF imported directly from Hugging Face repository ($repoId).",
            hfRepoId = repoId
        )
        modelRepository.addCustomModel(customModel)
    }
}
