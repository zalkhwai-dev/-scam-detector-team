package com.example.aiscamdetector.data.repository

import android.content.Context
import com.example.aiscamdetector.data.model.ScanResult
import com.example.aiscamdetector.data.model.Verdict
import com.example.aiscamdetector.integration.ApiError
import com.example.aiscamdetector.integration.NetworkClient
import com.example.aiscamdetector.integration.ScanHistoryDbHelper
import com.example.aiscamdetector.integration.ScanResponse

/**
 * Real implementation of Nada's ScanRepository interface, replacing
 * FixtureScanRepository. Wraps Zainab's NetworkClient (contract-matching
 * networking) and ScanHistoryDbHelper (on-device save) behind the interface
 * Nada's UI already expects - no UI code changes needed.
 */
class RealScanRepository(context: Context) : ScanRepository {

    private val dbHelper = ScanHistoryDbHelper(context.applicationContext)

    override suspend fun scanMessage(message: String): Result<ScanResult> {
        return try {
            val startTime = System.currentTimeMillis()
            val response = NetworkClient.analyzeMessage(message)
            val elapsedMs = (System.currentTimeMillis() - startTime).toDouble()
            dbHelper.saveResult(message, response, elapsedMs)
            Result.success(toScanResult(response, inputText = message))
        } catch (e: ApiError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun scanUrl(url: String): Result<ScanResult> {
        return try {
            val startTime = System.currentTimeMillis()
            val response = NetworkClient.checkUrl(url)
            val elapsedMs = (System.currentTimeMillis() - startTime).toDouble()
            dbHelper.saveResult(url, response, elapsedMs)
            Result.success(toScanResult(response, inputText = url))
        } catch (e: ApiError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Converts the contract-shaped ScanResponse into Nada's ScanResult.
     *
     * IMPORTANT re: riskScorePercent - per docs/decisions.md, risk_score must
     * never be invented; it stays null until a documented method exists.
     * For messages, model_score is a real model confidence value, so it's
     * used directly. For URLs, risk_score is currently always null (Amnah's
     * real scoring isn't built yet), so there is no honest number to show.
     * 0.0 here is a PLACEHOLDER ONLY because ScanResult.riskScorePercent is
     * non-nullable - it is paired with an explicit note in `summary` so nothing
     * implies a real score exists. Flag to Nada: this field may need to become
     * Double? so "not yet scored" can be shown honestly instead.
     */
    private fun toScanResult(response: ScanResponse, inputText: String): ScanResult {
        val verdict = when (response.scan_type) {
            "message" -> when (response.verdict) {
                "safe" -> Verdict.SAFE
                "scam" -> Verdict.SCAM
                else -> Verdict.UNKNOWN
            }
            "url" -> when (response.url_status) {
                "no_known_threat" -> Verdict.SAFE
                "threat_match" -> Verdict.SCAM
                else -> Verdict.UNKNOWN
            }
            else -> Verdict.UNKNOWN
        }

        // riskScorePercent is now Double? (per Nada's fix) - so we pass the
        // real value when one exists, and real null when it doesn't. No more
        // faking a 0.0 placeholder - per docs/decisions.md, an absent score
        // must stay absent, not be invented.
        val riskScorePercent: Double? = when {
            response.model_score != null -> response.model_score * 100
            response.risk_score != null -> response.risk_score.toDouble()
            else -> null
        }

        val suspiciousPhrases = response.highlights.mapNotNull { h ->
            if (h.start in inputText.indices && h.end in (h.start + 1)..inputText.length) {
                inputText.substring(h.start, h.end)
            } else null
        }

        val summary = response.summary ?: if (response.scan_type == "url" && response.risk_score == null) {
            "No numeric risk score available yet - result is based on a known-threat check only. " +
                "A no-known-threat result is not a safety guarantee."
        } else {
            null
        } ?: "No summary provided."

        // For a URL scan, the input itself is the detected URL. For a message
        // scan, no field in the contract carries this - the backend doesn't
        // extract URLs from message text - so we look for one directly in
        // what the user typed, the same simple pattern Nada's UrlFoundCard
        // expects to receive. If none is found, detectedUrl stays null and
        // UrlFoundCard correctly doesn't show.
        val detectedUrl = when (response.scan_type) {
            "url" -> inputText
            "message" -> urlPattern.find(inputText)?.value
            else -> null
        }

        return ScanResult(
            verdict = verdict,
            riskScorePercent = riskScorePercent,
            summary = summary,
            suspiciousPhrases = suspiciousPhrases,
            detectedUrl = detectedUrl
        )
    }

    companion object {
        private val urlPattern = Regex("""https?://\S+""")
    }
}
