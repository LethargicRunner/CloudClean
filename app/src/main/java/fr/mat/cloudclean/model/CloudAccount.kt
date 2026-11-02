package fr.mat.cloudclean.model

import org.json.JSONObject

/** Un compte connecte. Cle = accountId du fournisseur (jamais l'email). */
data class CloudAccount(
    val accountId: String,
    val provider: String,
    val displayName: String,
    val email: String,
    val accessToken: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("accountId", accountId)
        put("provider", provider)
        put("displayName", displayName)
        put("email", email)
        put("accessToken", accessToken)
    }

    companion object {
        fun fromJson(o: JSONObject) = CloudAccount(
            o.getString("accountId"),
            o.getString("provider"),
            o.getString("displayName"),
            o.getString("email"),
            o.getString("accessToken")
        )
    }
}