package fr.mat.cloudclean.google

import android.content.Context
import android.content.Intent
import android.net.Uri
import fr.mat.cloudclean.Constants
import fr.mat.cloudclean.http.HttpJsonClient
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * OAuth 2.0 code flow + PKCE pour Google Drive.
 * access_type=offline : un refresh_token est delivre (jetons d'acces ~1 h).
 */
class GoogleAuth(private val context: Context) {

    companion object {
        const val GOOGLE_CLIENT_ID = "INSERER_CLIENT_ID_ICI"
        const val GOOGLE_CLIENT_SECRET = "INSERER_CLIENT_SECRET_ICI"

        private const val PREFS = "cloudclean_google_state"
        private const val KEY_STATE = "pending_state_"

        private const val SCOPES =
            "https://www.googleapis.com/auth/drive.readonly " +
            "https://www.googleapis.com/auth/userinfo.profile " +
            "https://www.googleapis.com/auth/userinfo.email"
    }

    private fun pkce(): Pair<String, String> {
        val random = SecureRandom()
        val verifier = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(ByteArray(48).also(random::nextBytes))
        val challenge = Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-256")
                .digest(verifier.toByteArray(Charsets.US_ASCII))
        ).replace('+', '-').replace('/', '_').trimEnd('=')
        return verifier to challenge
    }

    /** Lance l'ecran de consentement Google (compte au choix de l'utilisateur). */
    fun startAuth(): Intent {
        val (verifier, challenge) = pkce()
        val random = SecureRandom()
        val state = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(ByteArray(16).also(random::nextBytes))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_STATE + state, verifier)
            .apply()

        val uri = Uri.parse(Constants.GOOGLE_AUTHORIZE)
            .buildUpon()
            .appendQueryParameter("client_id", GOOGLE_CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", Constants.REDIRECT_GOOGLE)
            .appendQueryParameter("scope", SCOPES)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("prompt", "consent")
            .appendQueryParameter("state", state)
            .build()
        return Intent(Intent.ACTION_VIEW, uri)
    }

    /** Traite le redirect : echange le code contre jetons, identifie le compte. */
    fun handleRedirect(uri: Uri): AuthResult {
        val state = uri.getQueryParameter("state")
            ?: throw AuthException("Redirect sans state")
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val verifier = prefs.getString(KEY_STATE + state, null)
            ?: throw AuthException("State inconnu (session PKCE perdue)")
        prefs.edit().remove(KEY_STATE + state).apply()

        val code = uri.getQueryParameter("code")
            ?: throw AuthException("Redirect sans code : " + uri.getQueryParameter("error"))

        val resp = HttpJsonClient.postForm(
            Constants.GOOGLE_TOKEN,
            mapOf(
                "code" to code,
                "client_id" to GOOGLE_CLIENT_ID,
                "client_secret" to GOOGLE_CLIENT_SECRET,
                "code_verifier" to verifier,
                "grant_type" to "authorization_code",
                "redirect_uri" to Constants.REDIRECT_GOOGLE
            )
        )
        if (resp.has("error")) {
            throw AuthException("Echange refuse : " + resp.optString("error"))
        }
        val refreshToken = resp.optString("refresh_token").ifEmpty {
            throw AuthException("Aucun refresh_token (access_type=offline ?)")
        }
        val accessToken = resp.getString("access_token")

        // Identite du compte
        val info = HttpJsonClient.getJson(Constants.GOOGLE_USERINFO, accessToken)
        return AuthResult(
            accountId = "google-" + info.getString("id"),
            displayName = info.optString("name").ifEmpty { info.optString("email") },
            email = info.optString("email"),
            refreshToken = refreshToken
        )
    }

    /** Rafraichit l'access token Google (expiration ~1 h). */
    fun refreshAccessToken(refreshToken: String): Pair<String, Long> {
        val resp = HttpJsonClient.postForm(
            Constants.GOOGLE_TOKEN,
            mapOf(
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
                "client_id" to GOOGLE_CLIENT_ID,
                "client_secret" to GOOGLE_CLIENT_SECRET
            )
        )
        if (resp.has("error")) {
            throw AuthException("Refresh refuse : " + resp.optString("error"))
        }
        val expiresInSeconds = resp.optLong("expires_in", 3600)
        return resp.getString("access_token") to
            (System.currentTimeMillis() + expiresInSeconds * 1000)
    }

    data class AuthResult(
        val accountId: String,
        val displayName: String,
        val email: String,
        val refreshToken: String
    )

    class AuthException(message: String) : Exception(message)
}