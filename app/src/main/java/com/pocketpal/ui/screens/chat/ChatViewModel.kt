package com.pocketpal.ui.screens.chat

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.data.model.ChatMessage
import com.pocketpal.data.model.ChatSession
import com.pocketpal.data.model.Pal
import com.pocketpal.data.repository.AiEngineService
import com.pocketpal.data.repository.ChatRepository
import com.pocketpal.data.repository.ModelRepository
import com.pocketpal.data.repository.PalRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val palRepository: PalRepository,
    private val modelRepository: ModelRepository,
    private val aiEngineService: AiEngineService
) : ViewModel() {

    val sessions: StateFlow<List<ChatSession>> = chatRepository.allSessions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val pals: StateFlow<List<Pal>> = palRepository.allPals.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val models = modelRepository.models
    val activeModelId = modelRepository.activeModelId

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId = _currentSessionId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _selectedPal = MutableStateFlow<Pal?>(null)
    val selectedPal = _selectedPal.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _temperature = MutableStateFlow(0.7f)
    val temperature = _temperature.asStateFlow()

    private val _attachedImageUri = MutableStateFlow<String?>(null)
    val attachedImageUri = _attachedImageUri.asStateFlow()

    private val _exportJson = MutableStateFlow<String?>(null)
    val exportJson = _exportJson.asStateFlow()

    private var generationJob: Job? = null
    private var messageObserverJob: Job? = null
    private var tts: TextToSpeech? = null
    private var isTtsSpeaking = false

    init {
        viewModelScope.launch {
            sessions.collect { sessionList ->
                if (_currentSessionId.value == null && sessionList.isNotEmpty()) {
                    selectSession(sessionList.first().id)
                } else if (sessionList.isEmpty()) {
                    createNewSession()
                }
            }
        }
    }

    fun initTts(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.US
                }
            }
        }
    }

    fun createNewSession(title: String = "New Chat", pal: Pal? = null) {
        viewModelScope.launch {
            val newSession = chatRepository.createNewSession(title, pal?.id, activeModelId.value)
            _currentSessionId.value = newSession.id
            _selectedPal.value = pal
            observeMessages(newSession.id)
        }
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
        observeMessages(sessionId)
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = null
            }
        }
    }

    fun selectPal(pal: Pal) {
        _selectedPal.value = pal
        _temperature.value = pal.temperature
    }

    fun selectModel(modelId: String) {
        modelRepository.selectActiveModel(modelId)
    }

    fun setTemperature(temp: Float) {
        _temperature.value = temp
    }

    fun setAttachedImage(uri: String?) {
        _attachedImageUri.value = uri
    }

    private fun observeMessages(sessionId: String) {
        messageObserverJob?.cancel()
        messageObserverJob = viewModelScope.launch {
            chatRepository.getMessagesForSession(sessionId).collect { list ->
                // Don't overwrite in-memory live streaming state if generation is active
                if (!_isGenerating.value) {
                    _messages.value = list
                }
            }
        }
    }

    fun sendMessage(userPrompt: String) {
        val sessionId = _currentSessionId.value ?: return
        if (userPrompt.isBlank() && _attachedImageUri.value == null) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = "user",
            content = userPrompt,
            timestamp = System.currentTimeMillis(),
            imageUri = _attachedImageUri.value
        )

        val imageUriToUse = _attachedImageUri.value
        _attachedImageUri.value = null

        viewModelScope.launch {
            // Persist user message to SQLite Room Database
            chatRepository.saveMessage(userMessage)

            // Add user message to UI state
            _messages.value = _messages.value + userMessage

            // Prepare Assistant response in-memory item
            val assistantMsgId = UUID.randomUUID().toString()
            val activeModelName = models.value.find { it.id == activeModelId.value }?.name ?: "Llama 3.2 1B (Offline)"

            val initialAssistantMsg = ChatMessage(
                id = assistantMsgId,
                sessionId = sessionId,
                role = "assistant",
                content = "",
                timestamp = System.currentTimeMillis(),
                isStreaming = true
            )

            // Append initial assistant placeholder in memory
            _messages.value = _messages.value + initialAssistantMsg
            _isGenerating.value = true
            val startTime = System.currentTimeMillis()

            generationJob = viewModelScope.launch {
                aiEngineService.generateStreamingResponse(
                    prompt = userPrompt,
                    pal = _selectedPal.value,
                    modelName = activeModelName,
                    temperature = _temperature.value,
                    hasImage = imageUriToUse != null,
                    history = _messages.value
                ).collect { accumulatedContent ->
                    val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
                    val estimatedTokens = accumulatedContent.length / 4
                    val tps = if (elapsedSec > 0) estimatedTokens / elapsedSec else 22.0

                    // Smooth in-memory update (Zero Room DB disk write spam!)
                    _messages.value = _messages.value.map { msg ->
                        if (msg.id == assistantMsgId) {
                            msg.copy(
                                content = accumulatedContent,
                                tokenCount = estimatedTokens,
                                speedTps = String.format("%.1f", tps).toDouble(),
                                thinkingTimeMs = System.currentTimeMillis() - startTime,
                                isStreaming = true
                            )
                        } else msg
                    }
                }

                // Finalize streaming and save completed message to SQLite Room DB once
                val finalAssistantMsg = _messages.value.find { it.id == assistantMsgId }?.copy(isStreaming = false)
                if (finalAssistantMsg != null) {
                    chatRepository.saveMessage(finalAssistantMsg)
                }

                _isGenerating.value = false

                // Speak aloud if TTS enabled
                if (isTtsSpeaking && finalAssistantMsg != null) {
                    tts?.speak(finalAssistantMsg.content, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        _isGenerating.value = false
    }

    fun clearChatHistory() {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch {
            chatRepository.clearSessionHistory(sessionId)
            _messages.value = emptyList()
        }
    }

    fun exportChatHistory() {
        viewModelScope.launch {
            val json = chatRepository.exportChatHistoryJson()
            _exportJson.value = json
        }
    }

    fun clearExportJson() {
        _exportJson.value = null
    }

    fun importChatHistory(jsonString: String) {
        viewModelScope.launch {
            val count = chatRepository.importChatHistoryJson(jsonString)
            if (count > 0 && sessions.value.isNotEmpty()) {
                selectSession(sessions.value.first().id)
            }
        }
    }

    fun toggleTtsPlayback(text: String) {
        if (tts?.isSpeaking == true) {
            tts?.stop()
            isTtsSpeaking = false
        } else {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            isTtsSpeaking = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
