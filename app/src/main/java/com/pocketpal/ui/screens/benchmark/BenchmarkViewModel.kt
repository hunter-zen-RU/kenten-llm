package com.pocketpal.ui.screens.benchmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.data.model.BenchmarkResult
import com.pocketpal.data.repository.BenchmarkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BenchmarkViewModel(private val benchmarkRepository: BenchmarkRepository) : ViewModel() {

    val results: StateFlow<List<BenchmarkResult>> = benchmarkRepository.allResults.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    private val _statusText = MutableStateFlow("")
    val statusText = _statusText.asStateFlow()

    fun runBenchmark(modelName: String) {
        if (_isRunning.value) return
        _isRunning.value = true
        _progress.value = 0f
        _statusText.value = "Starting benchmark..."

        viewModelScope.launch {
            try {
                benchmarkRepository.runBenchmark(modelName) { prog, status ->
                    _progress.value = prog
                    _statusText.value = status
                }
            } finally {
                _isRunning.value = false
            }
        }
    }
}
