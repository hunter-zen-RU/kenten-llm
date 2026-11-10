package com.pocketpal.ui.screens.voice

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.data.model.Pal
import com.pocketpal.data.repository.AiEngineService
import com.pocketpal.data.repository.PalRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING
}

class HandsFreeVoiceViewModel(
    private val palRepository: PalRepository,
    private val aiEngineService: AiEngineService
) : ViewModel() {

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState = _voiceState.asStateFlow()

    private val _userTranscript = MutableStateFlow("")
    val userTranscript = _userTranscript.asStateFlow()

    private val _aiResponse = MutableStateFlow("")
    val aiResponse = _aiResponse.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted = _isMuted.asStateFlow()

    private val _selectedPal = MutableStateFlow<Pal?>(null)
    val selectedPal = _selectedPal.asStateFlow()

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningLoopActive = false
    private var aiJob: Job? = null

    init {
        viewModelScope.launch {
            palRepository.allPals.collect { list ->
                if (_selectedPal.value == null && list.isNotEmpty()) {
                    _selectedPal.value = list.first()
                }
            }
        }
    }

    fun initVoiceEngines(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.US
                }
            }
        }

        if (SpeechRecognizer.isRecognitionAvailable(context) && speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _voiceState.value = VoiceState.LISTENING
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _voiceState.value = VoiceState.PROCESSING
                    }
                    override fun onError(error: Int) {
                        if (isListeningLoopActive && !_isMuted.value) {
                            startSimulatedListening()
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            processUserSpeech(matches[0])
                        } else if (isListeningLoopActive) {
                            startSimulatedListening()
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _userTranscript.value = matches[0]
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    fun startHandsFreeSession() {
        isListeningLoopActive = true
        _isMuted.value = false
        startSimulatedListening()
    }

    fun stopHandsFreeSession() {
        isListeningLoopActive = false
        speechRecognizer?.stopListening()
        tts?.stop()
        aiJob?.cancel()
        _voiceState.value = VoiceState.IDLE
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        if (_isMuted.value) {
            speechRecognizer?.stopListening()
            _voiceState.value = VoiceState.IDLE
        } else if (isListeningLoopActive) {
            startSimulatedListening()
        }
    }

    private fun startSimulatedListening() {
        if (_isMuted.value || !isListeningLoopActive) return
        _voiceState.value = VoiceState.LISTENING
        _userTranscript.value = "Listening for your voice..."
    }

    fun processUserSpeech(text: String) {
        if (text.isBlank()) return
        _userTranscript.value = text
        _voiceState.value = VoiceState.PROCESSING

        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            val responseSb = StringBuilder()
            aiEngineService.generateStreamingResponse(
                prompt = text,
                pal = _selectedPal.value,
                modelName = "Llama 3.2 1B (Offline)",
                temperature = 0.7f,
                hasImage = false,
                history = emptyList()
            ).collect { chunk ->
                responseSb.clear()
                responseSb.append(chunk)
                _aiResponse.value = chunk
            }

            // Speak AI reply aloud
            val fullReply = responseSb.toString()
            speakResponse(fullReply)
        }
    }

    private fun speakResponse(text: String) {
        _voiceState.value = VoiceState.SPEAKING
        val params = HashMap<String, String>()
        params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "PocketPalHandsFree"

        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                viewModelScope.launch {
                    delay(500)
                    if (isListeningLoopActive && !_isMuted.value) {
                        startSimulatedListening()
                    } else {
                        _voiceState.value = VoiceState.IDLE
                    }
                }
            }
            override fun onError(utteranceId: String?) {
                _voiceState.value = VoiceState.IDLE
            }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "PocketPalHandsFree")
    }

    override fun onCleared() {
        super.onCleared()
        stopHandsFreeSession()
        speechRecognizer?.destroy()
        tts?.shutdown()
    }
}
