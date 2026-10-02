package com.example.aiscamdetector.data.repository

import android.content.Context
import com.example.aiscamdetector.data.model.HistoryEntry
import com.example.aiscamdetector.data.model.ScanType
import com.example.aiscamdetector.data.model.Verdict
import com.example.aiscamdetector.integration.ScanHistoryDbHelper
import com.example.aiscamdetector.integration.ScanHistoryRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Real implementation of Nada's HistoryRepository interface, replacing
 * FixtureHistoryRepository. Reads from Zainab's on-device SQLite storage.
 */
class RealHistoryRepository(context: Context) : HistoryRepository {

    private val dbHelper = ScanHistoryDbHelper(context.applicationContext)
    private val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    override suspend fun getHistory(): Result<List<HistoryEntry>> {
        return try {
            val rows = dbHelper.readHistory()
            Result.success(rows.map { it.toHistoryEntry() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ScanHistoryRow.toHistoryEntry(): HistoryEntry {
        val type = if (scanType == "url") ScanType.URL else ScanType.MESSAGE

        val verdictEnum = when {
            scanType == "message" && verdict == "safe" -> Verdict.SAFE
            scanType == "message" && verdict == "scam" -> Verdict.SCAM
            scanType == "url" && urlStatus == "no_known_threat" -> Verdict.SAFE
            scanType == "url" && urlStatus == "threat_match" -> Verdict.SCAM
            else -> Verdict.UNKNOWN
        }

        return HistoryEntry(
            id = id.toString(),
            type = type,
            input = inputText,
            verdict = verdictEnum,
            riskScorePercent = when {
                modelScore != null -> modelScore * 100
                riskScore != null -> riskScore.toDouble()
                else -> null
            },
            summary = summary ?: urlSummary(scanType, urlStatus) ?: "No summary provided.",
            suspiciousPhrases = suspiciousPhrases,
            timestamp = dateFormat.format(Date(createdAt))
        )
    }

    companion object {
        /**
         * Plain-language explanation of a saved URL verdict, matching Use-
         * Case 3/4: the URL was checked against Google Safe Browsing's
         * database of known malicious/phishing sites and came back Safe,
         * Malicious, or Unavailable - never a numeric score, so this never
         * mentions one. Mirrors RealScanRepository's urlSummary() so a fresh
         * scan and its saved History entry read identically.
         */
        private fun urlSummary(scanType: String, urlStatus: String?): String? {
            if (scanType != "url") return null
            return when (urlStatus) {
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
