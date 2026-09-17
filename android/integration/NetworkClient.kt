package com.example.integrationtest.integration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets


object NetworkClient {

    suspend fun analyzeMessage(text: String): ScanResponse = withContext(Dispatchers.IO) {
        val body = JSONObject().put("text", text)
        val json = postJson("/api/v1/analyze-message", body)
        parseScanResponse(json)
    }

    suspend fun checkUrl(url: String): ScanResponse = withContext(Dispatchers.IO) {
        val body = JSONObject().put("url", url)
        val json = postJson("/api/v1/check-url", body)
        parseScanResponse(json)
    }

    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = openConnection("/health", "GET")
            val ok = conn.responseCode in 200..299
            conn.disconnect()
            ok
        } catch (e: Exception) {
            false
        }
    }

    // ---------- internal helpers ----------

    private fun postJson(path: String, body: JSONObject): JSONObject {
        val conn = openConnection(path, "POST")
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")

        conn.outputStream.use { os ->
            os.write(body.toString().toByteArray(StandardCharsets.UTF_8))
        }

        val responseCode = conn.responseCode
        val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
        val responseText = stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        conn.disconnect()

        val responseJson = JSONObject(responseText)

        if (responseCode !in 200..299) {
            // Per the contract: 422 invalid input, 503 unavailable, 504 timeout,
            // all using error.code / error.message.
            val error = responseJson.optJSONObject("error")
            throw ApiError(
                code = error?.optString("code") ?: responseCode.toString(),
                message = error?.optString("message") ?: "Request failed ($responseCode)"
            )
        }

        return responseJson
    }

    private fun openConnection(path: String, method: String): HttpURLConnection {
        val url = URL(BackendConfig.BASE_URL + path)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = BackendConfig.CONNECT_TIMEOUT_MS
        conn.readTimeout = BackendConfig.READ_TIMEOUT_MS
        return conn
    }

    private fun parseScanResponse(json: JSONObject): ScanResponse {
        val highlights = mutableListOf<Highlight>()
        val highlightsArray: JSONArray? = json.optJSONArray("highlights")
        if (highlightsArray != null) {
            for (i in 0 until highlightsArray.length()) {
                val h = highlightsArray.getJSONObject(i)
                highlights.add(Highlight(start = h.getInt("start"), end = h.getInt("end")))
            }
        }

        return ScanResponse(
            scan_type = json.getString("scan_type"),
            status = json.getString("status"),
            verdict = json.optStringOrNull("verdict"),
            model_label = json.optStringOrNull("model_label"),
            model_score = if (json.isNull("model_score")) null else json.optDouble("model_score"),
            model_version = json.optStringOrNull("model_version"),
            is_mock = json.optBoolean("is_mock", true),
            url_status = json.optStringOrNull("url_status"),
            risk_score = if (json.isNull("risk_score")) null else json.optInt("risk_score"),
            highlights = highlights,
            summary = json.optStringOrNull("summary")
        )
    }

    /** org.json has no built-in "give me a String or null", so this fills that gap. */
    private fun JSONObject.optStringOrNull(key: String): String? {
        return if (this.isNull(key) || !this.has(key)) null else this.optString(key)
    }
}
