package fr.mat.cloudclean.data

import fr.mat.cloudclean.model.CloudAccount
import fr.mat.cloudclean.model.CloudEntry
import fr.mat.cloudclean.pcloud.PCloudApi

/** Couche unique pour parcourir n'importe quel compte. S'ouvrira aux autres providers en V0.2+. */
class BrowseRepository {

    private val pcloud = PCloudApi()

    suspend fun list(account: CloudAccount, path: String): List<CloudEntry> =
        when (account.provider) {
            "pcloud" -> pcloud.listFolder(account.accessToken, path)
            else -> throw UnsupportedOperationException(
                "Provider " + account.provider + " : prevu en V0.2+"
            )
        }
}