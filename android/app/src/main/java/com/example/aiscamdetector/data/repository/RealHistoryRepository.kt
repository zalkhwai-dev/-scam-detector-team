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
            summary = summary ?: if (scanType == "url" && riskScore == null) {
                "No numeric risk score available yet - result is based on a known-threat check only."
            } else {
                "No summary provided."
            },
            timestamp = dateFormat.format(Date(createdAt))
        )
    }
}
