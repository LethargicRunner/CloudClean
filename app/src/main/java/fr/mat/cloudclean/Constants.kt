package fr.mat.cloudclean

object Constants {
    // API pCloud : US (api) et Europe (eapi) — la bonne region depend du compte.
    const val API_PCLOUD_US = "https://api.pcloud.com"
    const val API_PCLOUD_EU = "https://eapi.pcloud.com"
    const val PCLOUD_AUTHORIZE = "https://my.pcloud.com/oauth2/authorize"
    const val REDIRECT_URI = "cloudclean://oauth/callback"
}