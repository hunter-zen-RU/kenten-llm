package com.pocketpal.data.repository

import com.pocketpal.data.model.VoicePack
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VoiceEngineRepository {

    private val _voicePacks = MutableStateFlow<List<VoicePack>>(
        listOf(
            VoicePack(
                id = "vp_en_us_neural",
                name = "English (US) Neural Voice",
                type = "TTS Voice",
                language = "English (US)",
                sizeBytes = 47_185_920L,
                isDownloaded = true,
                isLoaded = true,
                description = "High-definition offline natural voice synthesis engine."
            ),
            VoicePack(
                id = "vp_whisper_stt",
                name = "Whisper Small Offline STT Engine",
                type = "STT Engine",
                language = "Multilingual (99 Languages)",
                sizeBytes = 157_286_400L,
                isDownloaded = true,
                isLoaded = true,
                description = "On-device speech recognition for zero-latency hands-free input."
            ),
            VoicePack(
                id = "vp_kokoro_tts",
                name = "Kokoro Neural Speech Pack",
                type = "Voice Model",
                language = "English & Spanish",
                sizeBytes = 89_128_960L,
                isDownloaded = false,
                isLoaded = false,
                description = "Ultra-realistic natural expressive voice pack for hands-free conversations."
            ),
            VoicePack(
                id = "vp_supertonic_engine",
                name = "Supertonic Expressive Voice Engine",
                type = "Voice Model",
                language = "English (UK / US)",
                sizeBytes = 115_343_360L,
                isDownloaded = false,
                isLoaded = false,
                description = "Lightweight fast neural speech model optimized for mobile CPUs."
            ),
            VoicePack(
                id = "vp_es_es_neural",
                name = "Spanish (ES) Neural Voice Pack",
                type = "TTS Voice",
                language = "Spanish",
                sizeBytes = 44_040_192L,
                isDownloaded = false,
                isLoaded = false,
                description = "Offline Spanish language voice synthesis pack."
            )
        )
    )
    val voicePacks: StateFlow<List<VoicePack>> = _voicePacks.asStateFlow()

    suspend fun downloadVoicePack(id: String, onProgress: (Float) -> Unit) {
        for (i in 1..10) {
            delay(250)
            val p = i / 10f
            onProgress(p)
            _voicePacks.update { list ->
                list.map { item ->
                    if (item.id == id) {
                        item.copy(
                            downloadProgress = p,
                            isDownloaded = p >= 1.0f
                        )
                    } else item
                }
            }
        }
    }

    fun toggleLoadVoicePack(id: String) {
        _voicePacks.update { list ->
            list.map { item ->
                if (item.id == id) {
                    item.copy(isLoaded = !item.isLoaded)
                } else item
            }
        }
    }
}
