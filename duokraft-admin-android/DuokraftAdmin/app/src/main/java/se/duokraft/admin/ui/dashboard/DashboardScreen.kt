package se.duokraft.admin.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import se.duokraft.admin.core.ApiResult
import se.duokraft.admin.data.AppContainer
import se.duokraft.admin.data.net.DashboardDto
import se.duokraft.admin.data.net.RecentItemDto
import se.duokraft.admin.data.net.SiteDto
import se.duokraft.admin.ui.appContainer
import se.duokraft.admin.ui.components.Badge
import se.duokraft.admin.ui.components.ErrorState
import se.duokraft.admin.ui.components.LoadingState
import se.duokraft.admin.ui.components.StatusBadge
import se.duokraft.admin.ui.components.formatWpDate
import se.duokraft.admin.ui.theme.DkBlue
import se.duokraft.admin.ui.theme.DkMuted

class DashboardViewModel(private val c: AppContainer) : ViewModel() {
    var site by mutableStateOf<SiteDto?>(c.site.site.value); private set
    var data by mutableStateOf<DashboardDto?>(null); private set
    var error by mutableStateOf<String?>(null); private set
    var refreshing by mutableStateOf(false); private set

    init { load() }

    fun load() {
        refreshing = true
        viewModelScope.launch {
            val s = c.site.refreshSite()
            val d = c.site.dashboard()
            if (s is ApiResult.Ok) site = s.value
            when (d) {
                is ApiResult.Ok -> { data = d.value; error = null }
                is ApiResult.Err -> error = d.message
            }
            refreshing = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    snackbar: SnackbarHostState,
    openList: (type: String) -> Unit,
    openEditor: (type: String, id: Long) -> Unit,
    openMedia: (upload: Boolean) -> Unit,
) {
    val c = appContainer()
    val vm: DashboardViewModel = viewModel { DashboardViewModel(c) }
    val data = vm.data

    LaunchedEffect(vm.error, data) {
        if (vm.error != null && data != null) snackbar.showSnackbar(vm.error!!)
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(vm.site?.name ?: "Dashboard", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    vm.site?.user?.name?.let { Text("Hi, $it", style = MaterialTheme.typography.bodySmall, color = DkMuted) }
                }
            },
            actions = { IconButton(onClick = vm::load) { Icon(Icons.Filled.Refresh, "Refresh") } },
        )
        when {
            data == null && vm.error != null -> ErrorState(vm.error!!, vm::load)
            data == null -> LoadingState()
            else -> PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = vm::load) {
                DashboardContent(vm.site, data, openList, openEditor, openMedia)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardContent(
    site: SiteDto?,
    data: DashboardDto,
    openList: (String) -> Unit,
    openEditor: (String, Long) -> Unit,
    openMedia: (Boolean) -> Unit,
) {
    val pages = data.types.firstOrNull { it.slug == "page" }
    val posts = data.types.firstOrNull { it.slug == "post" }
    val woo = site?.features?.woocommerce == true
    val products = data.types.firstOrNull { it.slug == "product" }

    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Pages", pages?.total(), Icons.Filled.Description) { openList("page") }
                StatCard("Posts", posts?.total(), Icons.Filled.Article) { openList("post") }
                if (woo && products != null) StatCard("Products", products.total(), Icons.Filled.Web) { openList("product") }
                StatCard("Drafts", data.totals["draft"] ?: 0, Icons.Filled.NoteAdd, null)
                StatCard("Scheduled", data.totals["future"] ?: 0, Icons.Filled.PostAdd, null)
                StatCard("Media", data.media.total, Icons.Filled.PhotoLibrary) { openMedia(false) }
            }
        }
        item {
            Section("Quick actions")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAction("Add page", Icons.Filled.NoteAdd) { openEditor("page", 0) }
                QuickAction("Add post", Icons.Filled.PostAdd) { openEditor("post", 0) }
                QuickAction("Upload image", Icons.Filled.AddPhotoAlternate) { openMedia(true) }
                if (woo) QuickAction("Add product", Icons.Filled.Web) { openEditor("product", 0) }
                QuickAction("Manage pages", Icons.Filled.Description) { openList("page") }
                QuickAction("Manage posts", Icons.Filled.Article) { openList("post") }
                QuickAction("Manage media", Icons.Filled.PhotoLibrary) { openMedia(false) }
            }
        }
        data.types.filter { it.recent.isNotEmpty() }.forEach { type ->
            item(key = "recent-${type.slug}") {
                Section("Recent ${type.name.lowercase()}")
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    type.recent.forEachIndexed { i, item ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        RecentRow(item) { openEditor(type.slug, item.id) }
                    }
                }
            }
        }
        if (data.media.recent.isNotEmpty()) item {
            Section("Recent uploads")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(data.media.recent, key = { it.id }) { m ->
                    AsyncImage(
                        model = m.thumbUrl ?: m.url,
                        contentDescription = m.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(84.dp).clip(RoundedCornerShape(8.dp)).clickable { openMedia(false) },
                    )
                }
            }
        }
    }
}

private fun se.duokraft.admin.data.net.DashTypeDto.total() =
    listOf("publish", "draft", "pending", "future", "private").sumOf { counts[it] ?: 0 }

@Composable
private fun StatCard(label: String, value: Int?, icon: ImageVector, onClick: (() -> Unit)?) {
    Card(
        modifier = Modifier.width(150.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = DkBlue)
            Text((value ?: 0).toString(), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(label, color = DkMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, onClick: () -> Unit) {
    androidx.compose.material3.AssistChip(onClick = onClick, label = { Text(label) }, leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) })
}

@Composable
private fun Section(title: String) {
    Text(title, Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun RecentRow(item: RecentItemDto, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        headlineContent = { Text(item.title.ifBlank { "(no title)" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(formatWpDate(item.modified), color = DkMuted) },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (item.builder == "elementor") Badge("Elementor", DkBlue)
                StatusBadge(item.status)
            }
        },
    )
}
