package fr.mat.cloudclean.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.mat.cloudclean.MainViewModel
import fr.mat.cloudclean.model.CloudAccount
import fr.mat.cloudclean.model.CloudEntry

@Composable
fun CloudCleanApp(vm: MainViewModel, onStartAddAccount: () -> Unit) {
    val account by vm.accountList.collectAsState()
    val current by vm.currentAccount.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val message by vm.message.collectAsState()

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.message.value = null }
    }

    MaterialTheme {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                if (current == null) AccountsScreen(vm, account, onStartAddAccount)
                else BrowseScreen(vm)
            }
        }
    }
}

@Composable
private fun AccountsScreen(
    vm: MainViewModel,
    accounts: List<CloudAccount>,
    onStartAddAccount: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Comptes connectes", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        if (accounts.isEmpty()) {
            Text(
                "Aucun compte. Ajoute un compte Dropbox pour commencer.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        LazyColumn {
            items(accounts, key = { it.accountId }) { acc ->
                ListItem(
                    headlineContent = { Text(acc.displayName) },
                    supportingContent = { Text(acc.provider + " - " + acc.email) },
                    leadingContent = { Icon(Icons.Default.AccountCircle, null) },
                    trailingContent = {
                        TextButton(onClick = { vm.disconnect(acc) }) { Text("Oublier") }
                    },
                    modifier = Modifier.clickable { vm.openAccount(acc) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onStartAddAccount) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Ajouter un compte Dropbox")
        }
    }
}

@Composable
private fun BrowseScreen(vm: MainViewModel) {
    val entries by vm.entries.collectAsState()
    val path by vm.path.collectAsState()
    val loading by vm.loading.collectAsState()
    val current by vm.currentAccount.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { vm.goUp() }, enabled = path.isNotEmpty()) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Dossier parent")
            }
            Text(
                if (path.isEmpty()) (current?.displayName ?: "") else path.last(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            if (path.isEmpty()) {
                TextButton(onClick = { current?.let(vm::disconnect) }) { Text("Comptes") }
            }
        }

        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())

        LazyColumn {
            items(entries, key = { it.id }) { entry: CloudEntry ->
                ListItem(
                    headlineContent = { Text(entry.name) },
                    supportingContent = {
                        Text(if (entry.isFolder) "Dossier" else formatSize(entry.size))
                    },
                    leadingContent = {
                        Icon(
                            if (entry.isFolder) Icons.Default.Folder
                            else Icons.Default.InsertDriveFile,
                            null
                        )
                    },
                    modifier = Modifier.clickable(enabled = entry.isFolder) {
                        vm.openFolder(entry.name)
                    }
                )
            }
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.1f Go".format(bytes / 1e9)
    bytes >= 1L shl 20 -> "%.1f Mo".format(bytes / 1e6)
    bytes >= 1L shl 10 -> "%.1f Ko".format(bytes / 1e3)
    else -> bytes.toString() + " o"
}