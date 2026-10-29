package se.duokraft.admin.ui.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import se.duokraft.admin.core.ApiResult
import se.duokraft.admin.data.AppContainer
import se.duokraft.admin.data.net.ContentTypeDto
import se.duokraft.admin.data.net.PostDto
import se.duokraft.admin.ui.appContainer
import se.duokraft.admin.ui.components.Badge
import se.duokraft.admin.ui.components.EmptyState
import se.duokraft.admin.ui.components.ErrorState
import se.duokraft.admin.ui.components.LoadingState
import se.duokraft.admin.ui.components.SearchField
import se.duokraft.admin.ui.components.StatusBadge
import se.duokraft.admin.ui.components.formatWpDate
import se.duokraft.admin.ui.components.wpText
import se.duokraft.admin.ui.theme.DkBlue
import se.duokraft.admin.ui.theme.DkMuted

private val STATUS_FILTERS = listOf(
    null to "All",
    "publish" to "Published",
    "draft" to "Drafts",
    "pending" to "Pending",
    "future" to "Scheduled",
    "private" to "Private",
)

class ContentListViewModel(private val c: AppContainer, private val slug: String) : ViewModel() {
    var type by mutableStateOf(c.site.type(slug)); private set
    val items = mutableStateListOf<PostDto>()
    var search by mutableStateOf(""); private set
    var status by mutableStateOf<String?>(null); private set
    var loading by mutableStateOf(true); private set
    var refreshing by mutableStateOf(false); private set
    var loadingMore by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    private var page = 1
    private var totalPages = 1
    private var job: Job? = null

    init { reload() }

    fun onSearch(value: String) {
        search = value
        job?.cancel()
        job = viewModelScope.launch { delay(400); load(reset = true) } // wait until the user stops typing
    }

    fun onStatus(value: String?) { status = value; reload() }

    fun reload(pull: Boolean = false) {
        job?.cancel()
        refreshing = pull
        job = viewModelScope.launch { load(reset = true) }
    }

    fun loadMore() {
        if (loadingMore || loading || page >= totalPages) return
        loadingMore = true
        viewModelScope.launch { load(reset = false) }
    }

    private suspend fun load(reset: Boolean) {
        if (type == null) {
            c.site.refreshSite()
            type = c.site.type(slug)
        }
        val t = type ?: run {
            error = "This content type is not available on the website."
            loading = false; refreshing = false; loadingMore = false
            return
        }
        if (reset) { loading = items.isEmpty() || !refreshing; page = 1 }
        val nextPage = if (reset) 1 else page + 1
        when (val r = c.content.list(t, nextPage, search, status)) {
            is ApiResult.Ok -> {
                if (reset) items.clear()
                items += r.value.items
                page = nextPage
                totalPages = r.value.totalPages
                error = null
            }
            is ApiResult.Err -> error = r.message
        }
        loading = false; refreshing = false; loadingMore = false
    }
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ContentListScreen(typeSlug: String, snackbar: SnackbarHostState, openEditor: (Long) -> Unit) {
    val c = appContainer()
    val vm: ContentListViewModel = viewModel(key = "list-$typeSlug") { ContentListViewModel(c, typeSlug) }
    val type = vm.type
    val listState = rememberLazyListState()

    // Infinite scroll: load the next page when the last items come into view.
    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 4
        }
    }
    LaunchedEffect(nearEnd) { if (nearEnd) vm.loadMore() }
    LaunchedEffect(vm.error) { if (vm.error != null && vm.items.isNotEmpty()) snackbar.showSnackbar(vm.error!!) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(title = { Text(type?.name ?: "Content") })
            SearchField(vm.search, vm::onSearch, "Search ${type?.name?.lowercase() ?: ""}", Modifier.padding(horizontal = 16.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(STATUS_FILTERS) { (value, label) ->
                    FilterChip(selected = vm.status == value, onClick = { vm.onStatus(value) }, label = { Text(label) })
                }
            }
            when {
                vm.loading -> LoadingState()
                vm.error != null && vm.items.isEmpty() -> ErrorState(vm.error!!, { vm.reload() })
                vm.items.isEmpty() -> EmptyState(
                    Icons.Filled.Description,
                    if (vm.search.isNotBlank()) "Nothing matches \"${vm.search}\"" else "No ${type?.name?.lowercase() ?: "items"} yet",
                    if (type?.can?.create == true) "Tap + to add one." else null,
                )
                else -> PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.reload(pull = true) }) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(vm.items, key = { it.id }) { post -> ContentCard(post, type) { openEditor(post.id) } }
                        if (vm.loadingMore) item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        }
                    }
                }
            }
        }
        if (type?.can?.create == true) {
            ExtendedFloatingActionButton(
                onClick = { openEditor(0) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add ${type.singularName.lowercase()}") },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }
}

@Composable
private fun ContentCard(post: PostDto, type: ContentTypeDto?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val thumb = post.embedded?.featuredMedia?.firstOrNull()?.thumbUrl
            if (type?.supports?.thumbnail == true) {
                Surface(Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)), color = MaterialTheme.colorScheme.surfaceVariant) {
                    if (thumb != null) AsyncImage(thumb, null, contentScale = ContentScale.Crop)
                    else Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Image, null, tint = DkMuted) }
                }
            }
            Column(Modifier.weight(1f).padding(start = if (type?.supports?.thumbnail == true) 12.dp else 0.dp)) {
                Text(
                    wpText(post.title.raw ?: post.title.rendered).ifBlank { "(no title)" },
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    (if (post.status == "future") "Scheduled for " + formatWpDate(post.date) else "Edited " + formatWpDate(post.modified)),
                    color = DkMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(post.status)
                    if (post.builder == "elementor") Badge("Elementor", DkBlue)
                }
            }
        }
    }
}
