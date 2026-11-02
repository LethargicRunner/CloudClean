package fr.mat.cloudclean

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.mat.cloudclean.data.AccountRepository
import fr.mat.cloudclean.data.BrowseRepository
import fr.mat.cloudclean.model.CloudAccount
import fr.mat.cloudclean.model.CloudEntry
import fr.mat.cloudclean.pcloud.PCloudAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val auth = PCloudAuth(app)
    private val accounts = AccountRepository(app)
    private val browseRepo = BrowseRepository()

    val accountList = MutableStateFlow<List<CloudAccount>>(emptyList())
    val currentAccount = MutableStateFlow<CloudAccount?>(null)
    val entries = MutableStateFlow<List<CloudEntry>>(emptyList())
    val path = MutableStateFlow<List<String>>(emptyList())
    val loading = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)

    init { reloadAccounts() }

    fun reloadAccounts() { accountList.value = accounts.list() }

    fun auth(): PCloudAuth = auth

    fun openAccount(account: CloudAccount) {
        currentAccount.value = account
        path.value = emptyList()
        load()
    }

    fun openFolder(name: String) {
        path.value = path.value + name
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

    /** Appele depuis MainActivity quand le redirect OAuth atterrit. */
    fun onAuthRedirect(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val result = auth.handleRedirect(uri)
                accounts.upsert(
                    CloudAccount(
                        accountId = result.accountId,
                        provider = "pcloud",
                        displayName = result.displayName,
                        email = result.email,
                        accessToken = result.accessToken
                    )
                )
                reloadAccounts()
                message.value = "Compte connecte : " + result.displayName
            } catch (e: Exception) {
                message.value = "Echec connexion : " + e.message
            }
        }
    }

    private fun load() {
        val account = currentAccount.value ?: return
        val pcloudPath = path.value.joinToString("") { "/" + it }
        viewModelScope.launch {
            loading.value = true
            entries.value = try {
                browseRepo.list(account, pcloudPath)
            } catch (e: Exception) {
                message.value = "Erreur : " + e.message
                emptyList()
            }
            loading.value = false
        }
    }
}