package com.pocketpal.data.repository

import com.pocketpal.data.model.AiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ModelRepository {

    private val _models = MutableStateFlow<List<AiModel>>(
        listOf(
            AiModel(
                id = "gemini-flash",
                name = "Gemini 1.5 Flash (Cloud)",
                provider = "Firebase Cloud",
                sizeBytes = 0,
                isDownloaded = true,
                isLoaded = true,
                quantization = "Cloud API",
                maxContext = 128000,
                description = "Ultra-fast multimodal cloud intelligence powered by Gemini 1.5 Flash."
            ),
            AiModel(
                id = "llama-3.2-1b",
                name = "Llama 3.2 1B Instruct",
                provider = "Local GGUF",
                sizeBytes = 1_180_000_000L,
                isDownloaded = true,
                isLoaded = false,
                quantization = "Q4_K_M",
                maxContext = 8192,
                description = "Compact 1B model optimized for ultra-fast on-device text generation.",
                hfRepoId = "meta-llama/Llama-3.2-1B-Instruct-GGUF"
            ),
            AiModel(
                id = "qwen-2.5-1.5b",
                name = "Qwen 2.5 1.5B Instruct",
                provider = "Local GGUF",
                sizeBytes = 1_450_000_000L,
                isDownloaded = false,
                isLoaded = false,
                quantization = "Q4_K_M",
                maxContext = 16384,
                description = "State-of-the-art 1.5B model with exceptional coding and logic capabilities.",
                hfRepoId = "Qwen/Qwen2.5-1.5B-Instruct-GGUF"
            ),
            AiModel(
                id = "gemma-2-2b",
                name = "Gemma 2 2B IT",
                provider = "Local GGUF",
                sizeBytes = 1_920_000_000L,
                isDownloaded = false,
                isLoaded = false,
                quantization = "Q4_K_M",
                maxContext = 8192,
                description = "Google's lightweight open model for high-fidelity mobile conversations.",
                hfRepoId = "google/gemma-2-2b-it-GGUF"
            ),
            AiModel(
                id = "phi-3.5-mini",
                name = "Phi-3.5 Mini 3.8B",
                provider = "Local GGUF",
                sizeBytes = 2_400_000_000L,
                isDownloaded = false,
                isLoaded = false,
                quantization = "Q4_K_M",
                maxContext = 128000,
                description = "Microsoft's high-reasoning 3.8B model for detailed synthesis.",
                hfRepoId = "microsoft/Phi-3.5-mini-instruct-GGUF"
            )
        )
    )
    val models: StateFlow<List<AiModel>> = _models.asStateFlow()

    private val _activeModelId = MutableStateFlow("gemini-flash")
    val activeModelId: StateFlow<String> = _activeModelId.asStateFlow()

    fun selectActiveModel(modelId: String) {
        _activeModelId.value = modelId
        _models.update { list ->
            list.map { model ->
                model.copy(isLoaded = (model.id == modelId))
            }
        }
    }

    fun updateDownloadProgress(modelId: String, progress: Float) {
        _models.update { list ->
            list.map { model ->
                if (model.id == modelId) {
                    model.copy(
                        downloadProgress = progress,
                        isDownloaded = progress >= 1.0f
                    )
                } else model
            }
        }
    }

    fun addCustomModel(model: AiModel) {
        _models.update { list ->
            list + model
        }
    }
}
