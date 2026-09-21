package com.example.aiscamdetector.data.repository

import com.example.aiscamdetector.data.model.ScanResult
import com.example.aiscamdetector.data.model.Verdict
import kotlinx.coroutines.delay

class FixtureScanRepository : ScanRepository {

    override suspend fun scanUrl(url: String): Result<ScanResult> {
        delay(900)
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter a URL to scan."))
        }
        return when {
            trimmed.contains("free-iphone-claim", ignoreCase = true) -> Result.success(
                ScanResult(
                    verdict = Verdict.SCAM,
                    riskScorePercent = 100.0,
                    summary = "The AI model classified this URL as likely a scam " +
                        "(confidence 100.0%). It matches a known scam-link pattern.",
                    suspiciousPhrases = listOf("free-iphone-claim.com")
                )
            )
            trimmed.contains("google.com", ignoreCase = true) -> Result.success(
                ScanResult(
                    verdict = Verdict.SAFE,
                    riskScorePercent = 5.0,
                    summary = "No known threats found in Google's Safe Browsing database."
                )
            )
            else -> Result.success(
                ScanResult(
                    verdict = Verdict.UNKNOWN,
                    riskScorePercent = 0.0,
                    summary = "No fixture data yet ",
                )
            )
        }
    }

    override suspend fun scanMessage(message: String): Result<ScanResult> {
        delay(900)
        val trimmed = message.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter a message to scan."))
        }
        val detectedUrl = extractFirstUrl(trimmed)
        return when {
            trimmed.contains("free iphone", ignoreCase = true) ||
                trimmed.contains("congratulations", ignoreCase = true) -> Result.success(
                ScanResult(
                    verdict = Verdict.SCAM,
                    riskScorePercent = 100.0,
                    summary = "The AI model classified this message as likely a scam " +
                        "(confidence 100.0%). Suspicious phrases found: Congratulations, " +
                        (detectedUrl ?: "a claimed prize link") + ".",
                    suspiciousPhrases = listOfNotNull("Congratulations", detectedUrl),
                    detectedUrl = detectedUrl
                )
            )
            else -> Result.success(
                ScanResult(
                    verdict = Verdict.UNKNOWN,
                    riskScorePercent = 0.0,
                    summary = "No fixture data yet ",
                    detectedUrl = detectedUrl
                )
            )
        }
    }

    companion object {

        private val URL_REGEX = Regex(
            "((https?://)?[\\w.-]+\\.[a-z]{2,}(/[\\w\\-./?%&=]*)?)",
            RegexOption.IGNORE_CASE
        )

        fun extractFirstUrl(text: String): String? {
            val match = URL_REGEX.find(text) ?: return null
            val raw = match.value
            return if (raw.startsWith("http://") || raw.startsWith("https://")) raw
            else "http://$raw"
        }
    }
}
