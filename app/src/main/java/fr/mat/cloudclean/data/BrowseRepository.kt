package fr.mat.cloudclean.data

import fr.mat.cloudclean.google.GoogleApi
import fr.mat.cloudclean.google.GoogleAuth
import fr.mat.cloudclean.model.CloudAccount
import fr.mat.cloudclean.model.CloudEntry
import fr.mat.cloudclean.pcloud.PCloudApi

/** Couche unique pour parcourir n'importe quel compte, par id de dossier. */
class BrowseRepository(googleAuth: GoogleAuth) {

    private val pcloud = PCloudApi()
    private val google = GoogleApi(googleAuth)

    suspend fun list(account: CloudAccount, folderId: String): List<CloudEntry> =
        when (account.provider) {
            "pcloud" -> pcloud.listFolder(account.accessToken, folderId)
            "google" -> google.listFolder(
                account.accountId,
                account.refreshToken ?: throw IllegalStateException("Compte Google sans refresh token"),
                folderId
            )
            else -> throw UnsupportedOperationException(
                "Provider " + account.provider + " : prevu en V0.4+"
            )
        }
}