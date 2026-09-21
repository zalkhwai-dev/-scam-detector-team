package com.example.aiscamdetector.data.model

enum class ScanType { URL, MESSAGE }

enum class Verdict { SAFE, SCAM, UNKNOWN }

data class ScanResult(
    val verdict: Verdict,
    val riskScorePercent: Double?,     // was: Double
    val summary: String,
    val suspiciousPhrases: List<String> = emptyList(),
    val detectedUrl: String? = null
)
