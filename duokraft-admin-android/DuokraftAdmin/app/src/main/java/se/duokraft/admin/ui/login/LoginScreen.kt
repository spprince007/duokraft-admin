package se.duokraft.admin.ui.login

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import se.duokraft.admin.AuthCallbacks
import se.duokraft.admin.R
import se.duokraft.admin.core.ApiResult
import se.duokraft.admin.data.AppContainer
import se.duokraft.admin.ui.appContainer
import se.duokraft.admin.ui.theme.DkMuted

class LoginViewModel(private val c: AppContainer) : ViewModel() {
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var done by mutableStateOf(false); private set

    fun loginUrl(): Uri = c.auth.authorizeUrl()

    fun onCallback(uri: Uri) {
        busy = true
        error = null
        viewModelScope.launch {
            when (val r = c.auth.handleCallback(uri)) {
                is ApiResult.Ok -> { c.site.remember(r.value); done = true }
                is ApiResult.Err -> error = r.message
            }
            busy = false
        }
    }
}

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    val c = appContainer()
    val vm: LoginViewModel = viewModel { LoginViewModel(c) }
    val context = LocalContext.current
    val callback by AuthCallbacks.pending.collectAsStateWithLifecycle()

    LaunchedEffect(callback) {
        callback?.let { AuthCallbacks.pending.value = null; vm.onCallback(it) }
    }
    LaunchedEffect(vm.done) { if (vm.done) onLoggedIn() }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(120.dp))
        Text("Duokraft Admin", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Manage your website from your phone.",
            Modifier.padding(top = 8.dp),
            color = DkMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))

        if (vm.busy) {
            CircularProgressIndicator()
            Text("Signing you in…", Modifier.padding(top = 12.dp), color = DkMuted)
        } else {
            Button(
                onClick = { CustomTabsIntent.Builder().build().launchUrl(context, vm.loginUrl()) },
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).height(52.dp),
            ) {
                Icon(Icons.Filled.Lock, null)
                Text("Log in with WordPress", Modifier.padding(start = 8.dp))
            }
            Text(
                "You'll log in on your website's own login page and approve this app. " +
                    "Your password is never stored on this phone.",
                Modifier.padding(top = 16.dp).widthIn(max = 420.dp),
                color = DkMuted,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
        vm.error?.let {
            Text(it, Modifier.padding(top = 20.dp), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
    }
}
