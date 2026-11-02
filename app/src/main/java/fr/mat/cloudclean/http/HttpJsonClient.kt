package fr.mat.cloudclean.http

import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

/** Tout le HTTP concentre ici. Seule classe a remplacer si bascule vers OkHttp un jour. */
object HttpJsonClient {

    class HttpError(
        val code: Int,
        val body: String?,
        val retryAfterSeconds: Int? = null
    ) : Exception("HTTP " + code + (retryAfterSeconds?.let { " (retry after " + it + " s)" } ?: "") + " : " + (body?.take(200) ?: ""))

    /** POST JSON (API RPC Dropbox). Retente automatiquement sur 429 (Retry-After) et 5xx. */
    fun postJson(
        url: String,
        bearerToken: String? = null,
        body: JSONObject,
        retries: Int = 2
    ): JSONObject = postWithRetry(
        url, bearerToken,
        body.toString().toByteArray(Charsets.UTF_8),
        "application/json", retries
    )

    /** POST form-urlencoded (echange et refresh du jeton OAuth). */
    fun postForm(
        url: String,
        form: Map<String, String>,
        retries: Int = 2
    ): JSONObject {
        val encoded = form.entries.joinToString("&") {
            URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
        }
        return postWithRetry(
            url, null,
            encoded.toByteArray(Charsets.UTF_8),
            "application/x-www-form-urlencoded", retries
        )
    }

    private fun postWithRetry(
        url: String,
        bearerToken: String?,
        payload: ByteArray,
        contentType: String,
        retries: Int
    ): JSONObject {
        var attempt = 0
        while (true) {
            try {
                return postOnce(url, bearerToken, payload, contentType)
            } catch (e: HttpError) {
                val retryable = e.code == 429 || e.code >= 500
                if (!retryable || attempt >= retries) throw e
                val delay = (e.retryAfterSeconds ?: 2) * 1000L
                Thread.sleep(delay)
                attempt++
            }
        }
    }

    private fun postOnce(
        url: String,
        bearerToken: String?,
        payload: ByteArray,
        contentType: String
    ): JSONObject {
        val conn = URL(url).openConnection() as HttpsURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 30_000
            conn.readTimeout = 60_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", contentType)
            bearerToken?.let { conn.setRequestProperty("Authorization", "Bearer " + it) }
            conn.outputStream.use { it.write(payload) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }

            if (code == 429) {
                throw HttpError(code, text, conn.getHeaderField("Retry-After")?.trim()?.toIntOrNull())
            }
            if (code !in 200..299) throw HttpError(code, text)
            return JSONObject(text ?: "{}")
        } finally {
            conn.disconnect()
        }
    }
}