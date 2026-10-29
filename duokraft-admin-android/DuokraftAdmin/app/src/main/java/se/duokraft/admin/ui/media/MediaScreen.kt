package se.duokraft.admin.ui.media

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import se.duokraft.admin.core.ApiResult
import se.duokraft.admin.data.AppContainer
import se.duokraft.admin.data.net.MediaDto
import se.duokraft.admin.ui.appContainer
import se.duokraft.admin.ui.components.ConfirmDialog
import se.duokraft.admin.ui.components.EmptyState
import se.duokraft.admin.ui.components.ErrorState
import se.duokraft.admin.ui.components.LoadingState
import se.duokraft.admin.ui.components.SearchField
import se.duokraft.admin.ui.components.formatWpDate
import se.duokraft.admin.ui.components.wpText
import se.duokraft.admin.ui.theme.DkMuted

/** Shared by the Media screen and the media picker used in the editor. */
class MediaViewModel(private val c: AppContainer) : ViewModel() {
    val items = mutableStateListOf<MediaDto>()
    var search by mutableStateOf(""); private set
    var loading by mutableStateOf(true); private set
    var refreshing by mutableStateOf(false); private set
    var loadingMore by mutableStateOf(false); private set
    var uploading by mutableStateOf(0); private set
    var error by mutableStateOf<String?>(null); private set
    var message by mutableStateOf<String?>(null)
    private var page = 1
    private var totalPages = 1
    private var job: Job? = null

    init { reload() }

    fun onSearch(value: String) {
        search = value
        job?.cancel()
        job = viewModelScope.launch { delay(400); load(true) }
    }

    fun reload(pull: Boolean = false) {
        job?.cancel(); refreshing = pull
        job = viewModelScope.launch { load(true) }
    }

    fun loadMore() {
        if (loadingMore || loading || page >= totalPages) return
        loadingMore = true
        viewModelScope.launch { load(false) }
    }

    private suspend fun load(reset: Boolean) {
        if (reset && !refreshing) loading = items.isEmpty()
        val next = if (reset) 1 else page + 1
        when (val r = c.media.list(next, search)) {
            is ApiResult.Ok -> {
                if (reset) items.clear()
                items += r.value.items; page = next; totalPages = r.value.totalPages; error = null
            }
            is ApiResult.Err -> error = r.message
        }
        loading = false; refreshing = false; loadingMore = false
    }

    /** Uploads (compressing first) and returns the new media item. */
    fun upload(uris: List<Uri>, onDone: (MediaDto) -> Unit = {}) {
        uris.forEach { uri ->
            uploading++
            viewModelScope.launch {
                when (val r = c.media.upload(uri)) {
                    is ApiResult.Ok -> { items.add(0, r.value); message = "Image uploaded"; onDone(r.value) }
                    is ApiResult.Err -> message = "Upload failed: ${r.message}"
                }
                uploading--
            }
        }
    }

    fun save(item: MediaDto, title: String, alt: String, caption: String, onSaved: () -> Unit) {
        viewModelScope.launch {
            when (val r = c.media.update(item.id, title, alt, caption)) {
                is ApiResult.Ok -> {
                    val i = items.indexOfFirst { it.id == item.id }
                    if (i >= 0) items[i] = r.value
                    message = "Saved"; onSaved()
                }
                is ApiResult.Err -> message = r.message
            }
        }
    }

    fun delete(item: MediaDto, onDeleted: () -> Unit) {
        viewModelScope.launch {
            when (val r = c.media.delete(item.id)) {
                is ApiResult.Ok -> { items.removeAll { it.id == item.id }; message = "Deleted"; onDeleted() }
                is ApiResult.Err -> message = r.message
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScreen(snackbar: SnackbarHostState, startUpload: Boolean) {
    val c = appContainer()
    val vm: MediaViewModel = viewModel(key = "media") { MediaViewModel(c) }
    var selected by remember { mutableStateOf<MediaDto?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) vm.upload(uris)
    }
    var autoOpened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(startUpload) {
        if (startUpload && !autoOpened) {
            autoOpened = true
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
    LaunchedEffect(vm.message) { vm.message?.let { snackbar.showSnackbar(it); vm.message = null } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(title = { Text("Media Library") })
            SearchField(vm.search, vm::onSearch, "Search media", Modifier.padding(horizontal = 16.dp))
            if (vm.uploading > 0) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                Text("Uploading ${vm.uploading} image(s)…", Modifier.padding(horizontal = 16.dp), color = DkMuted)
            }
            MediaGrid(vm, onClick = { selected = it }, bottomPadding = 96)
        }
        ExtendedFloatingActionButton(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            icon = { Icon(Icons.Filled.Upload, null) },
            text = { Text("Upload") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    selected?.let { item ->
        MediaDetailsSheet(item, vm, onDismiss = { selected = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaGrid(vm: MediaViewModel, onClick: (MediaDto) -> Unit, bottomPadding: Int = 16) {
    val state = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf { (state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= state.layoutInfo.totalItemsCount - 6 }
    }
    LaunchedEffect(nearEnd) { if (nearEnd) vm.loadMore() }

    when {
        vm.loading -> LoadingState()
        vm.error != null && vm.items.isEmpty() -> ErrorState(vm.error!!, { vm.reload() })
        vm.items.isEmpty() -> EmptyState(Icons.Filled.PhotoLibrary, "No images found", "Upload an image to get started.")
        else -> PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.reload(true) }) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(110.dp),
                state = state,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPadding.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(vm.items, key = { it.id }) { m ->
                    AsyncImage(
                        model = m.thumbUrl,
                        contentDescription = m.altText.ifBlank { wpText(m.title.rendered) },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                            .clickable { onClick(m) },
                    )
                }
                if (vm.loadingMore) item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaDetailsSheet(item: MediaDto, vm: MediaViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var title by remember(item.id) { mutableStateOf(item.title.raw ?: wpText(item.title.rendered)) }
    var alt by remember(item.id) { mutableStateOf(item.altText) }
    var caption by remember(item.id) { mutableStateOf(item.caption.raw ?: wpText(item.caption.rendered)) }
    var confirmDelete by remember { mutableStateOf(false) }
    val close: () -> Unit = { scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() } }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding()) {
            AsyncImage(
                item.sourceUrl, null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).clip(RoundedCornerShape(8.dp)),
            )
            val d = item.details
            Text(
                listOfNotNull(
                    item.mimeType,
                    if (d?.width != null && d.height != null) "${d.width} × ${d.height}" else null,
                    d?.filesize?.let { "${it / 1024} KB" },
                    formatWpDate(item.date),
                ).joinToString(" · "),
                Modifier.padding(vertical = 10.dp), color = DkMuted, style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true)
            OutlinedTextField(
                alt, { alt = it }, Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text("Alt text") }, supportingText = { Text("Describes the image for screen readers and search engines.") },
            )
            OutlinedTextField(caption, { caption = it }, Modifier.fillMaxWidth().padding(top = 4.dp), label = { Text("Caption") })
            Button(onClick = { vm.save(item, title, alt, caption, close) }, Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Save details") }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { copy(context, item.sourceUrl); vm.message = "URL copied" }, Modifier.weight(1f)) {
                    Icon(Icons.Filled.ContentCopy, null); Text(" Copy URL")
                }
                OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.sourceUrl))) }, Modifier.weight(1f)) {
                    Icon(Icons.Filled.OpenInNew, null); Text(" Open")
                }
            }
            OutlinedButton(onClick = { confirmDelete = true }, Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                Text(" Delete permanently", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (confirmDelete) ConfirmDialog(
        title = "Delete this image?",
        text = "It will be removed from the website permanently. Pages that show it will have a missing image.",
        confirmLabel = "Delete", destructive = true,
        onConfirm = { confirmDelete = false; vm.delete(item, close) },
        onDismiss = { confirmDelete = false },
    )
}

private fun copy(context: Context, text: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        .setPrimaryClip(ClipData.newPlainText("Media URL", text))
}
