package fr.mat.cloudclean

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import fr.mat.cloudclean.ui.CloudCleanApp

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CloudCleanApp(viewModel, onStartAddAccount = {
                startActivity(viewModel.auth().startAuth())
            })
        }
        handleIntent(intent)
    }

    /** singleTask : le redirect OAuth arrive par onNewIntent. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "cloudclean" && data.host == "oauth") {
            viewModel.onAuthRedirect(data)
        }
    }
}