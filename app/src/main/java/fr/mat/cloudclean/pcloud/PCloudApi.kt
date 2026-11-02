package fr.mat.cloudclean.pcloud

import fr.mat.cloudclean.Constants
import fr.mat.cloudclean.http.HttpJsonClient
import fr.mat.cloudclean.model.CloudEntry

/** Acces lecture seule aux fichiers pCloud. Erreurs API via champ "result" != 0. */
class PCloudApi {

    /**
     * Liste un dossier. path = chemin pCloud ("/" = racine).
     * La region (US/EU) du compte est decouverte automatiquement puis mise en cache.
     */
    suspend fun listFolder(accessToken: String, path: String): List<CloudEntry> {
        val encodedPath = if (path.isEmpty()) "/" else path
        val hosts = listOf(Constants.API_PCLOUD_US, Constants.API_PCLOUD_EU)
        var lastError: Exception? = null

        for (host in hosts) {
            try {
                val resp = HttpJsonClient.getJson(
                    host + "/listfolder?path=" +
                        java.net.URLEncoder.encode(encodedPath, "UTF-8") +
                        "&recursive=0&noshares=0&nofiles=0&showdeleted=0",
                    accessToken
                )
                if (resp.optInt("result", -1) != 0) {
                    lastError = PCloudApiException(
                        resp.optInt("result"), resp.optString("error")
                    )
                    continue // mauvaise region ou erreur temporaire : on tente l'autre host
                }
                return parseContents(resp)
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: PCloudApiException(-1, "Listage impossible")
    }

    private fun parseContents(resp: org.json.JSONObject): List<CloudEntry> {
        val contents = resp.optJSONObject("metadata")?.optJSONArray("contents")
            ?: return emptyList()
        return (0 until contents.length()).map { i ->
            val e = contents.getJSONObject(i)
            CloudEntry(
                id = e.getString("id"),
                name = e.getString("name"),
                pathLower = e.optString("path"),
                isFolder = e.optBoolean("isfolder"),
                size = e.optLong("size"),
                // Hash de contenu expose par l'API : base de la dedup sans telechargement
                contentHash = if (e.optLong("hash", -1L) == -1L) null else e.optLong("hash").toString(),
                lastModified = e.optString("modified").ifEmpty { null }
            )
        }.sortedWith(compareBy({ !it.isFolder }, { it.name.lowercase() }))
    }

    class PCloudApiException(val resultCode: Int, message: String) :
        Exception("pCloud result " + resultCode + " : " + message)
}