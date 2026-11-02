package fr.mat.cloudclean.dropbox

import android.content.Context
import android.content.Intent
import android.net.Uri
import fr.mat.cloudclean.Constants
import fr.mat.cloudclean.http.HttpJsonClient
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** OAuth 2.0 Authorization Code + PKCE pour Dropbox. Multi-comptes : un jeton par compte. */
class DropboxAuth(private val context: Context) {

    companion object {
        const val DROPBOX_APP_KEY = "INSERER_APP_KEY_ICI"

        private const val PREFS = "cloudclean_dropbox_pkce"
        private const val KEY_STATE = "pending_pkce_"
    }

    /** Verifier + challenge S256 + state aleatoires. */
    private fun pkce(): Triple<String, String, String> {
        val random = SecureRandom()
        val verifierBytes = ByteArray(48).also(random::nextBytes)
        val verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(verifierBytes)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        val challenge = Base64.getEncoder().encodeToString(digest)
            .replace('+', '-').replace('/', '_').trimEnd('=')
        val state = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(ByteArray(16).also(random::nextBytes))
        return Triple(verifier, challenge, state)
    }

    /** Lance l'ecran de connexion Dropbox (le compte choisi par l'utilisateur). */
    fun startAuth(): Intent {
        val (verifier, challenge, state) = pkce()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_STATE + state, verifier)
            .apply()

        val uri = Uri.parse(Constants.API_DROPBOX_OAUTH + "/oauth2/authorize")
            .buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", DROPBOX_APP_KEY)
            .appendQueryParameter("redirect_uri", Constants.REDIRECT_URI)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)
            .build()
        return Intent(Intent.ACTION_VIEW, uri)
    }

    /** Traite le redirect : echange le code contre refresh_token, identifie le compte. */
    fun handleRedirect(uri: Uri): AuthResult {
        val state = uri.getQueryParameter("state")
            ?: throw AuthException("Redirect sans state")
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val verifier = prefs.getString(KEY_STATE + state, null)
            ?: throw AuthException("State inconnu (session PKCE perdue)")
        prefs.edit().remove(KEY_STATE + state).apply()

        val code = uri.getQueryParameter("code")
            ?: throw AuthException("Redirect sans code : " + uri.getQueryParameter("error_description"))

        val tokenResp = HttpJsonClient.postForm(
            Constants.API_DROPBOX_OAUTH + "/oauth2/token",
            mapOf(
                "code" to code,
                "grant_type" to "authorization_code",
                "client_id" to DROPBOX_APP_KEY,
                "code_verifier" to verifier,
                "redirect_uri" to Constants.REDIRECT_URI
            )
        )
        val refreshToken = tokenResp.optString("refresh_token").ifEmpty {
            throw AuthException("Aucun refresh_token renvoye")
        }

        val accessToken = tokenResp.getString("access_token")
        val account = HttpJsonClient.postJson(
            "https://api.dropboxapi.com/2/users/get_current_account",
            accessToken, JSONObject()
        )
        return AuthResult(
            accountId = account.getString("account_id"),
            displayName = account.getJSONObject("name").optString("display_name")
                .ifEmpty { account.optString("email") },
            email = account.optString("email"),
            refreshToken = refreshToken
        )
    }

    /** Rafraichit l'access token (expiration ~4 h). */
    fun refreshAccessToken(refreshToken: String): String {
        val resp = HttpJsonClient.postForm(
            Constants.API_DROPBOX_OAUTH + "/oauth2/token",
            mapOf(
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
                "client_id" to DROPBOX_APP_KEY
            )
        )
        return resp.getString("access_token")
    }

    data class AuthResult(
        val accountId: String,
        val displayName: String,
        val email: String,
        val refreshToken: String
    )

    class AuthException(message: String) : Exception(message)
}