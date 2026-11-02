package fr.mat.cloudclean.dropbox

import fr.mat.cloudclean.Constants
import fr.mat.cloudclean.http.HttpJsonClient
import fr.mat.cloudclean.model.CloudEntry
import org.json.JSONObject

/** Acces lecture seule aux fichiers Dropbox. */
class DropboxApi(private val auth: DropboxAuth) {

    // accountId -> (access token, expiration ms)
    private val tokenCache = HashMap<String, Pair<String, Long>>()

    private fun accessToken(accountId: String, refreshToken: String): String {
        tokenCache[accountId]?.let { (token, expiry) ->
            if (System.currentTimeMillis() < expiry - 60_000) return token
        }
        val fresh = auth.refreshAccessToken(refreshToken)
        tokenCache[accountId] = fresh to (System.currentTimeMillis() + 4 * 3600_000L)
        return fresh
    }

    /** Liste un dossier, pagination geree. path = chemin Dropbox ("" = racine). */
    suspend fun listFolder(accountId: String, refreshToken: String, path: String): List<CloudEntry> {
        val token = accessToken(accountId, refreshToken)
        val body = JSONObject().put("path", path).put("recursive", false)

        var resp = HttpJsonClient.postJson(
            Constants.API_DROPBOX_API + "/2/files/list_folder", token, body
        )
        val entries = parseEntries(resp).toMutableList()
        var hasMore = resp.optBoolean("has_more", false)
        var cursor = resp.optString("cursor")

        while (hasMore) {
            resp = HttpJsonClient.postJson(
                Constants.API_DROPBOX_API + "/2/files/list_folder/continue",
                token, JSONObject().put("cursor", cursor)
            )
            entries += parseEntries(resp)
            hasMore = resp.optBoolean("has_more", false)
            cursor = resp.optString("cursor")
        }
        return entries.sortedWith(compareBy({ !it.isFolder }, { it.name.lowercase() }))
    }

    private fun parseEntries(resp: JSONObject): List<CloudEntry> {
        val arr = resp.optJSONArray("entries") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val e = arr.getJSONObject(i)
            when (e.getString(".tag")) {
                "folder" -> CloudEntry(
                    id = e.getString("id"),
                    name = e.getString("name"),
                    pathLower = e.getString("path_lower"),
                    isFolder = true, size = 0, contentHash = null, lastModified = null
                )
                "file" -> CloudEntry(
                    id = e.getString("id"),
                    name = e.getString("name"),
                    pathLower = e.getString("path_lower"),
                    isFolder = false,
                    size = e.optLong("size"),
                    // Base de la dedup future sans telechargement
                    contentHash = e.optString("content_hash").ifEmpty { null },
                    lastModified = e.optString("last_modified").ifEmpty { null }
                )
                else -> null
            }
        }
    }
}