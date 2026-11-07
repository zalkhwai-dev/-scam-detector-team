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
     * used directly. For URLs, there is no score at all, by design - Use-
     * Case 3/4 in the Senior I report defines URL checking as a Google Safe
     * Browsing threat-database lookup (Safe/Malicious/Unavailable), not a
     * scored prediction. riskScorePercent is null for every URL scan, and
     * the UI (ResultCard/HistoryScreen) only shows a "Risk score" line when
     * this is non-null, so a message scan's real score still displays and a
     * URL scan's absence of one is never described as "not yet available".
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

        // A risk score is shown only for a message the model judged to be a scam.
        // model_score is the model's confidence in its own verdict, so for a Scam
        // it is the probability that the message is a scam (a real risk). For a
        // Safe message it would be the confidence that it is safe, which is not a
        // risk, so nothing is shown. URLs never have a score.
        val riskScorePercent: Double? =
            if (response.scan_type == "message" && verdict == Verdict.SCAM && response.model_score != null)
                response.model_score * 100
            else null

        val suspiciousPhrases = response.highlights.mapNotNull { h ->
            if (h.start in inputText.indices && h.end in (h.start + 1)..inputText.length) {
                inputText.substring(h.start, h.end)
            } else null
        }

        val summary = response.summary ?: urlSummary(response) ?: "No summary provided."

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

        /**
         * Plain-language explanation of a URL verdict, matching Use-Case 3/4:
         * the URL is checked against Google Safe Browsing's database of known
         * malicious/phishing sites and comes back Safe, Malicious, or
         * Unavailable - never a numeric score, so this never mentions one.
         */
        private fun urlSummary(response: ScanResponse): String? {
            if (response.scan_type != "url") return null
            return when (response.url_status) {
                "no_known_threat" ->
                    "This URL was checked against Google Safe Browsing's database of known malicious and phishing sites, and no match was found. This is not a guarantee of safety - always use your own judgment, especially with sensitive information."
                "threat_match" ->
                    "This URL matches a known malicious or phishing site in Google's Safe Browsing database. Avoid visiting it."
                else ->
                    "URL verification is currently unavailable. Please try again later."
            }
        }
    }
}
