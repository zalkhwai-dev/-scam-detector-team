package com.example.aiscamdetector.data.repository

import com.example.aiscamdetector.data.model.HistoryEntry


interface HistoryRepository {
    suspend fun getHistory(): Result<List<HistoryEntry>>
}
