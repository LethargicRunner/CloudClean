package fr.mat.cloudclean.pcloud

import android.content.Context
import android.content.Intent
import android.net.Uri
import fr.mat.cloudclean.Constants
import fr.mat.cloudclean.http.HttpJsonClient
import java.security.SecureRandom
import java.util.Base64

/**
 * OAuth 2.0 code flow pCloud. Le jeton renvoye est permanent
 * (pas de refresh_token ni d'expiration a gerer). Multi-comptes : un jeton par compte.
 */
class PCloudAuth(private val context: Context) {

    companion object {
        const val PCLOUD_CLIENT_ID = "iOH6ajcrGWb"
        const val PCLOUD_CLIENT_SECRET = "9SLlqdQP6JH89GqrwIyR1XlBI8gX"

        private const val PREFS = "cloudclean_pcloud_state"
        private const val KEY_STATE = "pending_state_"
    }

    /** Lance l'ecran de connexion pCloud (le compte choisi par l'utilisateur). */
    fun startAuth(): Intent {
        val random = SecureRandom()
        val state = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(ByteArray(16).also(random::nextBytes))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_STATE + state, state)
            .apply()

        val uri = Uri.parse(Constants.PCLOUD_AUTHORIZE)
            .buildUpon()
            .appendQueryParameter("client_id", PCLOUD_CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", Constants.REDIRECT_PCLOUD)
            .appendQueryParameter("state", state)
            .build()
        return Intent(Intent.ACTION_VIEW, uri)
    }

    /** Traite le redirect : echange le code contre un access token, identifie le compte. */
    fun handleRedirect(uri: Uri): AuthResult {
        val state = uri.getQueryParameter("state")
            ?: throw AuthException("Redirect sans state")
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_STATE + state, null) == null) {
            throw AuthException("State inconnu (session OAuth perdue)")
        }
        prefs.edit().remove(KEY_STATE + state).apply()

        val code = uri.getQueryParameter("code")
            ?: throw AuthException("Redirect sans code : " + uri.getQueryParameter("error"))

        val tokenResp = HttpJsonClient.postForm(
            Constants.API_PCLOUD_US + "/oauth2_token",
            mapOf(
                "client_id" to PCLOUD_CLIENT_ID,
                "client_secret" to PCLOUD_CLIENT_SECRET,
                "code" to code
            )
        )
        if (tokenResp.optInt("result", 0) != 0) {
            throw AuthException("Echange du code refuse : " + tokenResp.optString("error"))
        }
        val accessToken = tokenResp.getString("access_token")
        val uid = tokenResp.optLong("uid")

        // Identite du compte (userinfo) — tente les deux regions.
        val info = HttpJsonClient.getJson(
            Constants.API_PCLOUD_US + "/userinfo", accessToken
        )
        if (info.optInt("result", 0) != 0) {
            HttpJsonClient.getJson(
                Constants.API_PCLOUD_EU + "/userinfo", accessToken
            )
        }

        return AuthResult(
            accountId = "pcloud-" + uid,
            displayName = info.optString("name").ifEmpty { info.optString("email") },
            email = info.optString("email"),
            accessToken = accessToken
        )
    }

    data class AuthResult(
        val accountId: String,
        val displayName: String,
        val email: String,
        val accessToken: String
    )

    class AuthException(message: String) : Exception(message)
}