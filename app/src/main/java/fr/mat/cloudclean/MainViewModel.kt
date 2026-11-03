package fr.mat.cloudclean

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.mat.cloudclean.data.AccountRepository
import fr.mat.cloudclean.data.BrowseRepository
import fr.mat.cloudclean.google.GoogleAuth
import fr.mat.cloudclean.model.CloudAccount
import fr.mat.cloudclean.model.CloudEntry
import fr.mat.cloudclean.pcloud.PCloudAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val pcloudAuth = PCloudAuth(app)
    private val googleAuth = GoogleAuth(app)
    private val accounts = AccountRepository(app)
    private val browseRepo = BrowseRepository(googleAuth)

    data class PathEntry(val name: String, val folderId: String)

    val accountList = MutableStateFlow<List<CloudAccount>>(emptyList())
    val currentAccount = MutableStateFlow<CloudAccount?>(null)
    val entries = MutableStateFlow<List<CloudEntry>>(emptyList())
    val path = MutableStateFlow<List<PathEntry>>(emptyList())
    val loading = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)

    init { reloadAccounts() }

    fun reloadAccounts() { accountList.value = accounts.list() }

    fun startAddPCloud(activity: android.app.Activity) {
        activity.startActivity(pcloudAuth.startAuth())
    }

    fun startAddGoogle(activity: android.app.Activity) {
        activity.startActivity(googleAuth.startAuth())
    }

    fun openAccount(account: CloudAccount) {
        currentAccount.value = account
        path.value = emptyList()
        load()
    }

    fun openFolder(name: String, folderId: String) {
        path.value = path.value + PathEntry(name, folderId)
        load()
    }

    fun goUp() {
        path.value = path.value.dropLast(1)
        load()
    }

    fun disconnect(account: CloudAccount) {
        accounts.remove(account.accountId)
        reloadAccounts()
        if (currentAccount.value?.accountId == account.accountId) currentAccount.value = null
    }

    /** Appele depuis MainActivity quand un redirect OAuth atterrit (pCloud ou Google). */
    fun onAuthRedirect(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                if (uri.path == "/google") {
                    val result = googleAuth.handleRedirect(uri)
                    // Recupere un access token pour d'eventuels usages immediats
                    accounts.upsert(
                        CloudAccount(
                            accountId = result.accountId,
                            provider = "google",
                            displayName = result.displayName,
                            email = result.email,
                            accessToken = "",
                            refreshToken = result.refreshToken
                        )
                    )
                    message.value = "Compte Google connecte : " + result.displayName
                } else {
                    val result = pcloudAuth.handleRedirect(uri)
                    accounts.upsert(
                        CloudAccount(
                            accountId = result.accountId,
                            provider = "pcloud",
                            displayName = result.displayName,
                            email = result.email,
                            accessToken = result.accessToken
                        )
                    )
                    message.value = "Compte pCloud connecte : " + result.displayName
                }
                reloadAccounts()
            } catch (e: Exception) {
                message.value = "Echec connexion : " + e.message
            }
        }
    }

    private fun load() {
        val account = currentAccount.value ?: return
        val folderId = path.value.lastOrNull()?.folderId ?: ""
        viewModelScope.launch {
            loading.value = true
            entries.value = try {
                browseRepo.list(account, folderId)
            } catch (e: Exception) {
                message.value = "Erreur : " + e.message
                emptyList()
            }
            loading.value = false
        }
    }
}