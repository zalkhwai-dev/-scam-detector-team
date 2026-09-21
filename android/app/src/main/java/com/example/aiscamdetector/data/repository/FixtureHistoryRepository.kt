package com.example.aiscamdetector.data.repository

import com.example.aiscamdetector.data.model.HistoryEntry
import kotlinx.coroutines.delay

class FixtureHistoryRepository : HistoryRepository {
    override suspend fun getHistory(): Result<List<HistoryEntry>> {
        delay(500)
        return Result.success(emptyList())
    }
}
