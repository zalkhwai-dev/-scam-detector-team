package com.example.aiscamdetector.ui.state

import com.example.aiscamdetector.data.model.ScanResult

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Loading : ScanUiState
    data class Success(val result: ScanResult) : ScanUiState
    data class Error(val message: String) : ScanUiState
}
