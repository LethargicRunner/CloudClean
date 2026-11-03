package fr.mat.cloudclean.google

import fr.mat.cloudclean.Constants
import fr.mat.cloudclean.http.HttpJsonClient
import fr.mat.cloudclean.model.CloudEntry
import java.net.URLEncoder

/** Acces lecture seule a Google Drive (API v3). md5Checksum = base de la dedup future. */
class GoogleApi(private val auth: GoogleAuth) {

    private const val FOLDER_MIME = "application/vnd.google-apps.folder"

    // accountId -> (access token, expiration ms)
    private val tokenCache = HashMap<String, Pair<String, Long>>()

    private suspend fun accessToken(accountId: String, refreshToken: String): String {
        tokenCache[accountId]?.let { (token, expiry) ->
            if (System.currentTimeMillis() < expiry - 60_000) return token
        }
        val (fresh, expiry) = auth.refreshAccessToken(refreshToken)
        tokenCache[accountId] = fresh to expiry
        return fresh
    }

    /** Liste un dossier (pagination geree). folderId vide = racine ("root"). */
    suspend fun listFolder(
        accountId: String, refreshToken: String, folderId: String
    ): List<CloudEntry> {
        val token = accessToken(accountId, refreshToken)
        val parent = if (folderId.isEmpty()) "root" else folderId
        val query = URLEncoder.encode(
            "'" + parent + "' in parents and trashed = false", "UTF-8"
        )
        val fields = URLEncoder.encode(
            "nextPageToken, files(id,name,mimeType,size,md5Checksum,modifiedTime)", "UTF-8"
        )

        val out = mutableListOf<CloudEntry>()
        var pageToken: String? = null
        do {
            var url = Constants.GOOGLE_DRIVE + "/files?pageSize=1000" +
                "&q=" + query + "&fields=" + fields +
                "&orderBy=" + URLEncoder.encode("folder,name", "UTF-8")
            pageToken?.let { url += "&pageToken=" + it }

            val resp = HttpJsonClient.getJson(url, token)
            if (resp.has("error")) {
                throw GoogleApiException(
                    resp.optJSONObject("error")?.optInt("code") ?: -1,
                    resp.optJSONObject("error")?.optString("message") ?: "?"
                )
            }
            resp.optJSONArray("files")?.let { arr ->
                (0 until arr.length()).forEach { i -> out += parseEntry(arr.getJSONObject(i)) }
            }
            pageToken = resp.optString("nextPageToken").ifEmpty { null }
        } while (pageToken != null)
        return out.sortedWith(compareBy({ !it.isFolder }, { it.name.lowercase() }))
    }

    private fun parseEntry(e: org.json.JSONObject): CloudEntry {
        val isFolder = e.optString("mimeType") == FOLDER_MIME
        val id = e.getString("id")
        return CloudEntry(
            id = id,
            name = e.getString("name"),
            folderId = if (isFolder) id else null,
            pathLower = "",
            isFolder = isFolder,
            size = e.optLong("size"),
            // md5Checksum : expose par Drive pour les fichiers binaires
            contentHash = e.optString("md5Checksum").ifEmpty { null },
            lastModified = e.optString("modifiedTime").ifEmpty { null }
        )
    }

    class GoogleApiException(val code: Int, message: String) :
        Exception("Drive error " + code + " : " + message)
}