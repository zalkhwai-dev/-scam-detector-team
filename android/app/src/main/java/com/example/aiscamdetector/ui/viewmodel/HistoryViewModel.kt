package com.example.aiscamdetector.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.aiscamdetector.data.model.HistoryEntry
import com.example.aiscamdetector.data.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Success(val entries: List<HistoryEntry>) : HistoryUiState
    data class Error(val message: String) : HistoryUiState
}

class HistoryViewModel(private val repository: HistoryRepository) : ViewModel() {

    private val _state = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = HistoryUiState.Loading
        viewModelScope.launch {
            repository.getHistory().fold(
                onSuccess = { _state.value = HistoryUiState.Success(it) },
                onFailure = { _state.value = HistoryUiState.Error(it.message ?: "Could not load history.") }
            )
        }
    }

    companion object {
        fun factory(repository: HistoryRepository) = viewModelFactory {
            initializer { HistoryViewModel(repository) }
        }
    }
}
