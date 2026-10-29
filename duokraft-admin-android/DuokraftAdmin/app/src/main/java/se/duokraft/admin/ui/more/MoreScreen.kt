package se.duokraft.admin.ui.more

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import se.duokraft.admin.BuildConfig
import se.duokraft.admin.ui.appContainer
import se.duokraft.admin.ui.components.Badge
import se.duokraft.admin.ui.components.ConfirmDialog
import se.duokraft.admin.ui.theme.DkMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(snackbar: SnackbarHostState, openList: (String) -> Unit, onLoggedOut: () -> Unit) {
    val c = appContainer()
    val site by c.site.site.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var askLogout by remember { mutableStateOf(false) }
    var loggingOut by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("More") })
        Column(Modifier.verticalScroll(rememberScrollState())) {
            ListItem(
                leadingContent = {
                    AsyncImage(site?.user?.avatarUrl, null, Modifier.size(44.dp).clip(CircleShape))
                },
                headlineContent = { Text(site?.user?.name ?: "") },
                supportingContent = { Text(site?.user?.roles?.joinToString()?.replaceFirstChar { it.uppercase() } ?: "", color = DkMuted) },
            )
            ListItem(
                modifier = Modifier.clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(site?.homeUrl ?: c.siteUrl))) },
                leadingContent = { Icon(Icons.Filled.OpenInNew, null) },
                headlineContent = { Text(site?.name ?: "Website") },
                supportingContent = { Text(site?.homeUrl ?: c.siteUrl, color = DkMuted) },
            )
            HorizontalDivider()

            // Any extra content types the site registers (custom post types) appear automatically.
            val extra = site?.contentTypes.orEmpty().filter { it.slug != "page" && it.slug != "post" }
            if (extra.isNotEmpty()) {
                Text("Content", Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp), style = MaterialTheme.typography.titleSmall, color = DkMuted)
                extra.forEach { t ->
                    ListItem(
                        modifier = Modifier.clickable { openList(t.slug) },
                        leadingContent = { Icon(Icons.Filled.Folder, null) },
                        headlineContent = { Text(t.name) },
                    )
                }
                HorizontalDivider()
            }

            Text("Website", Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp), style = MaterialTheme.typography.titleSmall, color = DkMuted)
            ListItem(
                leadingContent = { Icon(Icons.Filled.Tune, null) },
                headlineContent = { Text("Website settings & Front Page") },
                supportingContent = { Text("Logo, contact details, homepage texts", color = DkMuted) },
                trailingContent = { Badge("Next update", DkMuted) },
            )
            ListItem(
                leadingContent = { Icon(Icons.Filled.Menu, null) },
                headlineContent = { Text("Menus") },
                trailingContent = { Badge("Coming soon", DkMuted) },
            )
            HorizontalDivider()
            ListItem(
                modifier = Modifier.clickable(enabled = !loggingOut) { askLogout = true },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error) },
                headlineContent = { Text(if (loggingOut) "Logging out…" else "Log out", color = MaterialTheme.colorScheme.error) },
            )
            Text(
                "Duokraft Admin ${BuildConfig.VERSION_NAME}" + (site?.wpVersion?.let { " · WordPress $it" } ?: ""),
                Modifier.padding(16.dp), color = DkMuted, style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (askLogout) ConfirmDialog(
        title = "Log out?",
        text = "This phone's access to the website will be removed. You can log in again at any time.",
        confirmLabel = "Log out",
        onConfirm = {
            askLogout = false; loggingOut = true
            scope.launch {
                c.auth.logout()
                c.site.forget()
                loggingOut = false
                onLoggedOut()
                snackbar.showSnackbar("Logged out")
            }
        },
        onDismiss = { askLogout = false },
    )
}
