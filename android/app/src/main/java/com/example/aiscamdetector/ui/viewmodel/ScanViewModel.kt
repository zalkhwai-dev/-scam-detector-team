package com.example.aiscamdetector.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.aiscamdetector.data.repository.ScanRepository
import com.example.aiscamdetector.ui.state.ScanUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScanViewModel(private val repository: ScanRepository) : ViewModel() {

    var urlInput by mutableStateOf("")
        private set
    var messageInput by mutableStateOf("")
        private set

    private val _urlState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val urlState: StateFlow<ScanUiState> = _urlState.asStateFlow()

    private val _messageState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val messageState: StateFlow<ScanUiState> = _messageState.asStateFlow()

    fun onUrlInputChange(value: String) { urlInput = value }
    fun onMessageInputChange(value: String) { messageInput = value }

    fun scanUrl(url: String = urlInput) {
        if (url.isBlank()) {
            _urlState.value = ScanUiState.Error("Enter a URL to scan.")
            return
        }
        _urlState.value = ScanUiState.Loading
        viewModelScope.launch {
            repository.scanUrl(url).fold(
                onSuccess = { _urlState.value = ScanUiState.Success(it) },
                onFailure = {
                    // Never fall back to "Safe" - surface the failure as-is.
                    _urlState.value = ScanUiState.Error(it.message ?: "Something went wrong. Please try again.")
                }
            )
        }
    }

    fun scanMessage() {
        if (messageInput.isBlank()) {
            _messageState.value = ScanUiState.Error("Enter a message to scan.")
            return
        }
        _messageState.value = ScanUiState.Loading
        viewModelScope.launch {
            repository.scanMessage(messageInput).fold(
                onSuccess = { _messageState.value = ScanUiState.Success(it) },
                onFailure = {
                    _messageState.value = ScanUiState.Error(it.message ?: "Something went wrong. Please try again.")
                }
            )
        }
    }

    fun scanDetectedUrl(url: String) {
        urlInput = url
        scanUrl(url)
    }

    companion object {
        fun factory(repository: ScanRepository) = viewModelFactory {
            initializer { ScanViewModel(repository) }
        }
    }
}
