package com.pocketpal.data.model

data class ChatSession(
    val id: String,
    val title: String,
    val palId: String? = null,
    val modelId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String,
    val sessionId: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null,
    val tokenCount: Int = 0,
    val speedTps: Double = 0.0,
    val thinkingTimeMs: Long = 0,
    val isStreaming: Boolean = false
)

data class Pal(
    val id: String,
    val name: String,
    val avatar: String,
    val description: String,
    val systemPrompt: String,
    val category: String, // "Coding", "Writing", "Roleplay", "Smart Chat", "Education", "Custom"
    val isCustom: Boolean = false,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val iconName: String = "Sparkles"
)

data class AiModel(
    val id: String,
    val name: String,
    val provider: String, // "Local GGUF", "Firebase Cloud", "Hugging Face"
    val sizeBytes: Long,
    val isDownloaded: Boolean = false,
    val isLoaded: Boolean = false,
    val quantization: String = "Q4_K_M",
    val maxContext: Int = 4096,
    val description: String,
    val hfRepoId: String? = null,
    val downloadProgress: Float = 0f
)

data class BenchmarkResult(
    val id: String,
    val modelName: String,
    val tokensPerSec: Double,
    val ttftMs: Long,
    val ramUsageMb: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val hardwareCpu: String = "Octa-core ARM64",
    val hardwareRam: String = "8 GB"
)

data class ChatSettings(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val maxTokens: Int = 1024,
    val systemPrompt: String = "You are PocketPal, a helpful, friendly, and knowledgeable AI assistant.",
    val language: String = "English",
    val ttsEnabled: Boolean = false,
    val activeModelId: String = "llama-3.2-1b",
    val isOfflineOnlyMode: Boolean = true,
    val isLocalServerEnabled: Boolean = true,
    val localServerPort: Int = 8080
)

data class VoicePack(
    val id: String,
    val name: String,
    val type: String, // "TTS Voice", "STT Engine", "Voice Model"
    val language: String,
    val sizeBytes: Long,
    val isDownloaded: Boolean = false,
    val isLoaded: Boolean = false,
    val downloadProgress: Float = 0f,
    val description: String
)

data class ApiRequestLog(
    val id: String,
    val method: String, // "POST", "GET"
    val endpoint: String,
    val status: Int, // 200, 400, etc.
    val timestamp: Long = System.currentTimeMillis(),
    val clientIp: String = "127.0.0.1",
    val requestBodySnippet: String = "",
    val responseBodySnippet: String = ""
)
