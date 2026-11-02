package fr.mat.cloudclean.data

import fr.mat.cloudclean.dropbox.DropboxApi
import fr.mat.cloudclean.dropbox.DropboxAuth
import fr.mat.cloudclean.model.CloudAccount
import fr.mat.cloudclean.model.CloudEntry

/** Couche unique pour parcourir n'importe quel compte. S'ouvrira aux autres providers en V0.2+. */
class BrowseRepository(auth: DropboxAuth) {

    private val dropbox = DropboxApi(auth)

    suspend fun list(account: CloudAccount, path: String): List<CloudEntry> =
        when (account.provider) {
            "dropbox" -> dropbox.listFolder(account.accountId, account.refreshToken, path)
            else -> throw UnsupportedOperationException(
                "Provider " + account.provider + " : prevu en V0.2+"
            )
        }
}