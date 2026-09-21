package com.example.aiscamdetector.integration

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray

/**
 * On-device SQLite storage (Zainab's Day 1 task #3), per the provisional
 * decision in docs/decisions.md: the backend returns a result, and the
 * Android app saves it here. FastAPI never writes to this database.
 *
 * PROVISIONAL SCHEMA: Amnah's database_schema.sql (User, Scan, Result,
 * AI Model Log, URL Check entities from Section 5.5) had not been delivered
 * yet when this was written. This single-table schema stores exactly the
 * fields the agreed API contract returns, so it can be built and tested
 * immediately. Once Amnah's schema is ready, reconcile field names/tables
 * with her and update this file together - do not diverge silently.
 */
class ScanHistoryDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_SCANS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SCAN_TYPE TEXT NOT NULL,
                $COL_INPUT_TEXT TEXT NOT NULL,
                $COL_STATUS TEXT NOT NULL,
                $COL_VERDICT TEXT,
                $COL_MODEL_LABEL TEXT,
                $COL_MODEL_SCORE REAL,
                $COL_MODEL_VERSION TEXT,
                $COL_IS_MOCK INTEGER NOT NULL,
                $COL_URL_STATUS TEXT,
                $COL_RISK_SCORE INTEGER,
                $COL_SUMMARY TEXT,
                $COL_HIGHLIGHTS_JSON TEXT,
                $COL_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SCANS")
        onCreate(db)
    }

    /** Saves one scan result. Returns the new row's id. */
    fun saveResult(inputText: String, response: ScanResponse): Long {
        val values = ContentValues().apply {
            put(COL_SCAN_TYPE, response.scan_type)
            put(COL_INPUT_TEXT, inputText)
            put(COL_STATUS, response.status)
            put(COL_VERDICT, response.verdict)
            put(COL_MODEL_LABEL, response.model_label)
            response.model_score?.let { put(COL_MODEL_SCORE, it) }
            put(COL_MODEL_VERSION, response.model_version)
            put(COL_IS_MOCK, if (response.is_mock) 1 else 0)
            put(COL_URL_STATUS, response.url_status)
            response.risk_score?.let { put(COL_RISK_SCORE, it) }
            put(COL_SUMMARY, response.summary)
            put(COL_HIGHLIGHTS_JSON, highlightsToJson(response.highlights))
            put(COL_CREATED_AT, System.currentTimeMillis())
        }
        return writableDatabase.insert(TABLE_SCANS, null, values)
    }

    /** Returns all saved scans, most recent first. */
    fun readHistory(): List<ScanHistoryRow> {
        val rows = mutableListOf<ScanHistoryRow>()
        val cursor = readableDatabase.query(
            TABLE_SCANS, null, null, null, null, null,
            "$COL_CREATED_AT DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                rows.add(
                    ScanHistoryRow(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                        scanType = it.getString(it.getColumnIndexOrThrow(COL_SCAN_TYPE)),
                        inputText = it.getString(it.getColumnIndexOrThrow(COL_INPUT_TEXT)),
                        status = it.getString(it.getColumnIndexOrThrow(COL_STATUS)),
                        verdict = it.getStringOrNull(COL_VERDICT),
                        isMock = it.getInt(it.getColumnIndexOrThrow(COL_IS_MOCK)) == 1,
                        urlStatus = it.getStringOrNull(COL_URL_STATUS),
                        modelScore = it.getDoubleOrNull(COL_MODEL_SCORE),
                        riskScore = it.getIntOrNull(COL_RISK_SCORE),
                        summary = it.getStringOrNull(COL_SUMMARY),
                        createdAt = it.getLong(it.getColumnIndexOrThrow(COL_CREATED_AT))
                    )
                )
            }
        }
        return rows
    }

    private fun highlightsToJson(highlights: List<Highlight>): String {
        val arr = JSONArray()
        highlights.forEach {
            arr.put(org.json.JSONObject().put("start", it.start).put("end", it.end))
        }
        return arr.toString()
    }

    private fun android.database.Cursor.getStringOrNull(col: String): String? {
        val i = getColumnIndexOrThrow(col)
        return if (isNull(i)) null else getString(i)
    }

    private fun android.database.Cursor.getIntOrNull(col: String): Int? {
        val i = getColumnIndexOrThrow(col)
        return if (isNull(i)) null else getInt(i)
    }

    private fun android.database.Cursor.getDoubleOrNull(col: String): Double? {
        val i = getColumnIndexOrThrow(col)
        return if (isNull(i)) null else getDouble(i)
    }

    companion object {
        private const val DATABASE_NAME = "scan_history.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_SCANS = "scans"
        const val COL_ID = "id"
        const val COL_SCAN_TYPE = "scan_type"
        const val COL_INPUT_TEXT = "input_text"
        const val COL_STATUS = "status"
        const val COL_VERDICT = "verdict"
        const val COL_MODEL_LABEL = "model_label"
        const val COL_MODEL_SCORE = "model_score"
        const val COL_MODEL_VERSION = "model_version"
        const val COL_IS_MOCK = "is_mock"
        const val COL_URL_STATUS = "url_status"
        const val COL_RISK_SCORE = "risk_score"
        const val COL_SUMMARY = "summary"
        const val COL_HIGHLIGHTS_JSON = "highlights_json"
        const val COL_CREATED_AT = "created_at"
    }
}

/** One row as read back from the local database - used to feed Nada's history screen. */
data class ScanHistoryRow(
    val id: Long,
    val scanType: String,
    val inputText: String,
    val status: String,
    val verdict: String?,
    val isMock: Boolean,
    val urlStatus: String?,
    val modelScore: Double?,
    val riskScore: Int?,
    val summary: String?,
    val createdAt: Long
)
