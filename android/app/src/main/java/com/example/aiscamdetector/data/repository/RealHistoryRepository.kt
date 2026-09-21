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
 *
 * NOTE: requires ScanHistoryDbHelper's readHistory() to also return url_status
 * per scan (see the ScanHistoryDbHelper.kt update alongside this file) -
 * without it, URL scans in history always show as UNKNOWN verdict, since
 * `verdict` in the database is only ever set for messages, never URLs.
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
            // Double? per Nada's fix - real null when no real score exists,
            // never a faked 0.0 (see docs/decisions.md on not inventing risk_score).
            riskScorePercent = when {
                modelScore != null -> modelScore * 100
                riskScore != null -> riskScore.toDouble()
                else -> null
            },
            timestamp = dateFormat.format(Date(createdAt))
        )
    }
}
