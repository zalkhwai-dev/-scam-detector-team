package com.example.integrationtest.integration


data class Highlight(
    val start: Int,
    val end: Int
)

/**
 * The single response shape used for BOTH /analyze-message and /check-url,
 */
data class ScanResponse(
    val scan_type: String,       // "message" or "url"
    val status: String,          // "completed" or "needs_review"
    val verdict: String?,        // "safe" / "scam" for messages; null for urls (for now)
    val model_label: String?,
    val model_score: Double?,    // 0-1, confidence - NOT accuracy
    val model_version: String?,
    val is_mock: Boolean,
    val url_status: String?,     // "threat_match" / "no_known_threat"
    val risk_score: Int?,        // 0-100, null until a method is documented
    val highlights: List<Highlight>,
    val summary: String?
)

/** Thrown by NetworkClient on 422/503/504 or any other backend error response. */
data class ApiError(
    val code: String,
    override val message: String
) : Exception(message)
