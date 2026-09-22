package com.example.aiscamdetector.integration

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * On-device SQLite storage (Zainab's Day 1 task #3).
 *
 * RECONCILED with Amnah's real database_schema.sql, plus two corrections
 * against the Senior I report's Section 5.5 ERD (see CHANGES_FOR_AMNAH.md):
 * - users: username -> full_name + email (report says "name, email, and
 *   password"; Nada's LoginScreen already collects email, not a username)
 * - ai_model_logs: added processing_time (report explicitly promises this;
 *   it wasn't in the delivered schema). Measured client-side in
 *   RealScanRepository, since neither the AI model nor Rana's backend
 *   response currently returns it - see COL_PROCESSING_TIME_MS below.
 *
 * DECISION MADE (was an open question, resolved 22 Sep): ai_model_logs and
 * url_checks link to `results`, not `scans` directly, keeping Amnah's
 * original choice over the report diagram's design. Reasoning: saveResult()
 * only ever inserts a log/check together with a Result, in one transaction,
 * only on a successful response - there is no case in this app where a log
 * exists without a Result, so linking to Scan directly would add no real
 * flexibility. Semantically, these tables represent the evidence that
 * produces the one Result each scan has, so nesting them under Result fits.
 *
 * This file's public functions (saveResult, readHistory) keep their shape
 * as close to the original as possible so RealScanRepository.kt only needed
 * one small addition (passing measured processingTimeMs in), not a rewrite.
 *
 * STILL OPEN, not resolved here: `user_id` is left NULL on every scan -
 * it's unclear whether Nada's AuthViewModel writes to the `users` table at
 * all, or whether email/password here are meant to connect to it. Confirm
 * with Amnah and Nada before assuming a real link.
 */
class ScanHistoryDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                $COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_FULL_NAME TEXT NOT NULL,
                $COL_EMAIL TEXT NOT NULL,
                $COL_PASSWORD_HASH TEXT NOT NULL,
                $COL_USER_CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_SCANS (
                $COL_SCAN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SCAN_USER_ID INTEGER,
                $COL_INPUT_TYPE TEXT NOT NULL,
                $COL_INPUT_TEXT TEXT NOT NULL,
                $COL_SCAN_TIMESTAMP TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY ($COL_SCAN_USER_ID) REFERENCES $TABLE_USERS($COL_USER_ID)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_RESULTS (
                $COL_RESULT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_RESULT_SCAN_ID INTEGER,
                $COL_VERDICT TEXT,
                $COL_RISK_SCORE REAL,
                $COL_SUMMARY TEXT,
                FOREIGN KEY ($COL_RESULT_SCAN_ID) REFERENCES $TABLE_SCANS($COL_SCAN_ID)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_AI_MODEL_LOGS (
                $COL_LOG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_LOG_RESULT_ID INTEGER,
                $COL_MODEL_LABEL TEXT,
                $COL_MODEL_SCORE REAL,
                $COL_MODEL_VERSION TEXT,
                $COL_IS_MOCK BOOLEAN,
                $COL_PROCESSING_TIME_MS REAL,
                FOREIGN KEY ($COL_LOG_RESULT_ID) REFERENCES $TABLE_RESULTS($COL_RESULT_ID)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_URL_CHECKS (
                $COL_URLCHECK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_URLCHECK_RESULT_ID INTEGER,
                $COL_URL TEXT NOT NULL,
                $COL_URL_STATUS TEXT,
                FOREIGN KEY ($COL_URLCHECK_RESULT_ID) REFERENCES $TABLE_RESULTS($COL_RESULT_ID)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_URL_CHECKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_AI_MODEL_LOGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RESULTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SCANS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    /**
     * Saves one scan result across scans/results/ai_model_logs (and
     * url_checks, for URL scans), in a single transaction. Returns the new
     * scan's id.
     *
     * processingTimeMs is measured by the CALLER (RealScanRepository, timing
     * the actual NetworkClient request/response round trip) since neither
     * the AI model nor the backend currently reports this themselves. Pass
     * null if not measured.
     */
    fun saveResult(inputText: String, response: ScanResponse, processingTimeMs: Double? = null): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val scanValues = ContentValues().apply {
                put(COL_INPUT_TYPE, response.scan_type)
                put(COL_INPUT_TEXT, inputText)
                put(COL_SCAN_TIMESTAMP, System.currentTimeMillis())
            }
            val scanId = db.insert(TABLE_SCANS, null, scanValues)

            val resultValues = ContentValues().apply {
                put(COL_RESULT_SCAN_ID, scanId)
                put(COL_VERDICT, response.verdict)
                response.risk_score?.let { put(COL_RISK_SCORE, it.toDouble()) }
                put(COL_SUMMARY, response.summary)
            }
            val resultId = db.insert(TABLE_RESULTS, null, resultValues)

            val logValues = ContentValues().apply {
                put(COL_LOG_RESULT_ID, resultId)
                put(COL_MODEL_LABEL, response.model_label)
                response.model_score?.let { put(COL_MODEL_SCORE, it) }
                put(COL_MODEL_VERSION, response.model_version)
                put(COL_IS_MOCK, if (response.is_mock) 1 else 0)
                processingTimeMs?.let { put(COL_PROCESSING_TIME_MS, it) }
            }
            db.insert(TABLE_AI_MODEL_LOGS, null, logValues)

            if (response.scan_type == "url") {
                val urlValues = ContentValues().apply {
                    put(COL_URLCHECK_RESULT_ID, resultId)
                    put(COL_URL, inputText)
                    put(COL_URL_STATUS, response.url_status)
                }
                db.insert(TABLE_URL_CHECKS, null, urlValues)
            }

            db.setTransactionSuccessful()
            return scanId
        } finally {
            db.endTransaction()
        }
    }

    /** Returns all saved scans, most recent first, joined back into one row per scan. */
    fun readHistory(): List<ScanHistoryRow> {
        val rows = mutableListOf<ScanHistoryRow>()
        val query = """
            SELECT
                s.$COL_SCAN_ID AS scan_id,
                s.$COL_INPUT_TYPE AS input_type,
                s.$COL_INPUT_TEXT AS input_text,
                s.$COL_SCAN_TIMESTAMP AS scan_timestamp,
                r.$COL_VERDICT AS verdict,
                r.$COL_RISK_SCORE AS risk_score,
                r.$COL_SUMMARY AS summary,
                a.$COL_MODEL_SCORE AS model_score,
                a.$COL_IS_MOCK AS is_mock,
                a.$COL_PROCESSING_TIME_MS AS processing_time_ms,
                u.$COL_URL_STATUS AS url_status
            FROM $TABLE_SCANS s
            LEFT JOIN $TABLE_RESULTS r ON r.$COL_RESULT_SCAN_ID = s.$COL_SCAN_ID
            LEFT JOIN $TABLE_AI_MODEL_LOGS a ON a.$COL_LOG_RESULT_ID = r.$COL_RESULT_ID
            LEFT JOIN $TABLE_URL_CHECKS u ON u.$COL_URLCHECK_RESULT_ID = r.$COL_RESULT_ID
            ORDER BY s.$COL_SCAN_TIMESTAMP DESC
        """.trimIndent()

        val cursor = readableDatabase.rawQuery(query, null)
        cursor.use {
            while (it.moveToNext()) {
                rows.add(
                    ScanHistoryRow(
                        id = it.getLong(it.getColumnIndexOrThrow("scan_id")),
                        scanType = it.getString(it.getColumnIndexOrThrow("input_type")),
                        inputText = it.getString(it.getColumnIndexOrThrow("input_text")),
                        status = "completed",
                        verdict = it.getStringOrNull("verdict"),
                        isMock = it.getIntOrNull("is_mock") == 1,
                        urlStatus = it.getStringOrNull("url_status"),
                        modelScore = it.getDoubleOrNull("model_score"),
                        riskScore = it.getDoubleOrNull("risk_score"),
                        processingTimeMs = it.getDoubleOrNull("processing_time_ms"),
                        summary = it.getStringOrNull("summary"),
                        createdAt = it.getLong(it.getColumnIndexOrThrow("scan_timestamp"))
                    )
                )
            }
        }
        return rows
    }

    private fun Cursor.getStringOrNull(col: String): String? {
        val i = getColumnIndexOrThrow(col)
        return if (isNull(i)) null else getString(i)
    }

    private fun Cursor.getIntOrNull(col: String): Int? {
        val i = getColumnIndexOrThrow(col)
        return if (isNull(i)) null else getInt(i)
    }

    private fun Cursor.getDoubleOrNull(col: String): Double? {
        val i = getColumnIndexOrThrow(col)
        return if (isNull(i)) null else getDouble(i)
    }

    companion object {
        private const val DATABASE_NAME = "scan_history.db"
        private const val DATABASE_VERSION = 3

        const val TABLE_USERS = "users"
        const val COL_USER_ID = "id"
        const val COL_FULL_NAME = "full_name"
        const val COL_EMAIL = "email"
        const val COL_PASSWORD_HASH = "password_hash"
        const val COL_USER_CREATED_AT = "created_at"

        const val TABLE_SCANS = "scans"
        const val COL_SCAN_ID = "id"
        const val COL_SCAN_USER_ID = "user_id"
        const val COL_INPUT_TYPE = "input_type"
        const val COL_INPUT_TEXT = "input_text"
        const val COL_SCAN_TIMESTAMP = "timestamp"

        const val TABLE_RESULTS = "results"
        const val COL_RESULT_ID = "id"
        const val COL_RESULT_SCAN_ID = "scan_id"
        const val COL_VERDICT = "verdict"
        const val COL_RISK_SCORE = "risk_score"
        const val COL_SUMMARY = "summary"

        const val TABLE_AI_MODEL_LOGS = "ai_model_logs"
        const val COL_LOG_ID = "id"
        const val COL_LOG_RESULT_ID = "result_id"
        const val COL_MODEL_LABEL = "model_label"
        const val COL_MODEL_SCORE = "model_score"
        const val COL_MODEL_VERSION = "model_version"
        const val COL_IS_MOCK = "is_mock"
        const val COL_PROCESSING_TIME_MS = "processing_time"

        const val TABLE_URL_CHECKS = "url_checks"
        const val COL_URLCHECK_ID = "id"
        const val COL_URLCHECK_RESULT_ID = "result_id"
        const val COL_URL = "url"
        const val COL_URL_STATUS = "url_status"
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
    val riskScore: Double?,
    val processingTimeMs: Double?,
    val summary: String?,
    val createdAt: Long
)
