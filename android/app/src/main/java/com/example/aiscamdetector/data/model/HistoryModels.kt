package com.example.aiscamdetector.data.model

data class HistoryEntry(
    val id: String,
    val type: ScanType,
    val input: String,
    val verdict: Verdict,
    val riskScorePercent: Double?,     // was: Double
    val timestamp: String
)
