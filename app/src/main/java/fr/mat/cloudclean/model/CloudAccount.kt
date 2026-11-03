package fr.mat.cloudclean.model

import org.json.JSONObject

/** Un compte connecte. Cle = accountId du fournisseur (jamais l'email). */
data class CloudAccount(
    val accountId: String,
    val provider: String,
    val displayName: String,
    val email: String,
    /** Jeton permanent (pCloud) ou jeton d'acces Google. */
    val accessToken: String,
    /** Google uniquement : necessaire pour rafraichir les jetons ~1 h. */
    val refreshToken: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("accountId", accountId)
        put("provider", provider)
        put("displayName", displayName)
        put("email", email)
        put("accessToken", accessToken)
        put("refreshToken", refreshToken ?: "")
    }

    companion object {
        fun fromJson(o: JSONObject) = CloudAccount(
            o.getString("accountId"),
            o.getString("provider"),
            o.getString("displayName"),
            o.getString("email"),
            o.getString("accessToken"),
            o.optString("refreshToken").ifEmpty { null }
        )
    }
}