package com.example.aiscamdetector.data.model

data class HistoryEntry(
    val id: String,
    val type: ScanType,
    val input: String,
    val verdict: Verdict,
    val riskScorePercent: Double?,
    val summary: String,
    val suspiciousPhrases: List<String> = emptyList(),
    val timestamp: String
)
