package se.duokraft.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.flow.MutableStateFlow
import se.duokraft.admin.ui.AppNav
import se.duokraft.admin.ui.theme.DuokraftTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleAuthIntent(intent)
        setContent {
            DuokraftTheme { AppNav() }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "duokraftadmin" && data.host == "auth") {
            AuthCallbacks.pending.value = data
            setIntent(Intent(this, MainActivity::class.java)) // don't replay on rotation
        }
    }
}

/** Hands the WordPress login redirect from the Activity to the login screen. */
object AuthCallbacks {
    val pending = MutableStateFlow<Uri?>(null)
}
