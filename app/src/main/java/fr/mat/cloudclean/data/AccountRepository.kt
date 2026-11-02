package fr.mat.cloudclean.data

import android.content.Context
import fr.mat.cloudclean.model.CloudAccount
import org.json.JSONArray

/** Persistance des comptes connectes. Multi-comptes natif : 1 entree par accountId. */
class AccountRepository(context: Context) {

    private val prefs = context.getSharedPreferences("cloudclean_accounts", Context.MODE_PRIVATE)

    fun list(): List<CloudAccount> {
        val raw = prefs.getString("accounts", null) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { CloudAccount.fromJson(arr.getJSONObject(it)) }
    }

    /** Ajoute ou remplace un compte (meme accountId = reconnexion, pas de doublon). */
    fun upsert(account: CloudAccount) {
        val accounts = list().filterNot { it.accountId == account.accountId } + account
        saveAll(accounts)
    }

    fun remove(accountId: String) = saveAll(list().filterNot { it.accountId == accountId })

    private fun saveAll(accounts: List<CloudAccount>) {
        prefs.edit().putString("accounts", JSONArray().apply {
            accounts.forEach { put(it.toJson()) }
        }.toString()).apply()
    }
}