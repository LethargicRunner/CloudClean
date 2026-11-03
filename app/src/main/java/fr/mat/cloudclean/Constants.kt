package fr.mat.cloudclean

object Constants {
    // API pCloud : US (api) et Europe (eapi) — la bonne region depend du compte.
    const val API_PCLOUD_US = "https://api.pcloud.com"
    const val API_PCLOUD_EU = "https://eapi.pcloud.com"
    const val PCLOUD_AUTHORIZE = "https://my.pcloud.com/oauth2/authorize"

    // API Google Drive
    const val GOOGLE_AUTHORIZE = "https://accounts.google.com/o/oauth2/v2/auth"
    const val GOOGLE_TOKEN = "https://oauth2.googleapis.com/token"
    const val GOOGLE_DRIVE = "https://www.googleapis.com/drive/v3"
    const val GOOGLE_USERINFO = "https://www.googleapis.com/oauth2/v2/userinfo"

    const val REDIRECT_PCLOUD = "cloudclean://oauth/callback"
    const val REDIRECT_GOOGLE = "cloudclean://oauth/google"
}