package se.duokraft.admin.ui.content

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import se.duokraft.admin.data.net.TaxonomyDto
import se.duokraft.admin.ui.appContainer
import se.duokraft.admin.ui.components.ConfirmDialog
import se.duokraft.admin.ui.components.ErrorState
import se.duokraft.admin.ui.components.LoadingState
import se.duokraft.admin.ui.components.StatusBadge
import se.duokraft.admin.ui.components.formatWpDate
import se.duokraft.admin.ui.media.MediaPickerDialog
import se.duokraft.admin.ui.theme.DkBlue
import se.duokraft.admin.ui.theme.DkMuted
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentEditScreen(typeSlug: String, id: Long?, snackbar: SnackbarHostState, onClose: () -> Unit) {
    val c = appContainer()
    val vm: ContentEditViewModel = viewModel(key = "edit-$typeSlug-$id") { ContentEditViewModel(c, typeSlug, id) }
    val type = vm.type
    val context = LocalContext.current

    var askLeave by remember { mutableStateOf(false) }
    var askTrash by remember { mutableStateOf(false) }
    var pickFeatured by remember { mutableStateOf(false) }
    var scheduling by remember { mutableStateOf(false) }

    val tryClose = { if (vm.isDirty) askLeave = true else onClose() }
    BackHandler(enabled = vm.isDirty) { askLeave = true }
    LaunchedEffect(vm.message) { vm.message?.let { snackbar.showSnackbar(it); vm.message = null } }
    LaunchedEffect(vm.closeRequested) { if (vm.closeRequested) onClose() }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopAppBar(
            title = { Text((if (vm.id == null) "New " else "Edit ") + (type?.singularName?.lowercase() ?: "item")) },
            navigationIcon = { IconButton(onClick = tryClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            actions = {
                vm.link?.let { link ->
                    IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }) {
                        Icon(Icons.Filled.OpenInNew, "View on website")
                    }
                }
                if (vm.id != null && type?.can?.delete == true) {
                    IconButton(onClick = { askTrash = true }) { Icon(Icons.Filled.Delete, "Move to trash") }
                }
            },
        )
        if (vm.saving) LinearProgressIndicator(Modifier.fillMaxWidth())

        when {
            vm.loading -> LoadingState()
            vm.loadError != null -> ErrorState(vm.loadError!!, vm::load)
            type != null -> {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // ---------- Basic information ----------
                    SectionCard("Basic information") {
                        if (type.supports.title) OutlinedTextField(
                            vm.title, { vm.title = it }, Modifier.fillMaxWidth(),
                            label = { Text("Title") }, singleLine = true,
                        )
                        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Status: ", color = DkMuted)
                            StatusBadge(if (vm.id == null) "draft" else vm.status)
                            vm.scheduledAt?.let { Text("  ${formatWpDate(it.toString())}", color = DkMuted) }
                        }
                    }

                    // ---------- Content ----------
                    if (type.supports.editor) {
                        if (vm.isElementor) ElementorNotice(vm.isFrontPage)
                        else SectionCard("Content") { HtmlEditor(vm, c.siteUrl) }
                    }
                    if (type.supports.excerpt) SectionCard("Excerpt") {
                        OutlinedTextField(
                            vm.excerpt, { vm.excerpt = it }, Modifier.fillMaxWidth().heightIn(min = 96.dp),
                            supportingText = { Text("Short summary shown in lists and search results.") },
                        )
                    }

                    // ---------- Featured image ----------
                    if (type.supports.thumbnail) SectionCard("Featured image") {
                        if (vm.featuredId > 0 && vm.featuredUrl != null) {
                            AsyncImage(
                                vm.featuredUrl, null, contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)),
                            )
                            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { pickFeatured = true }) { Text("Change") }
                                TextButton(onClick = { vm.featuredId = 0; vm.featuredUrl = null }) { Text("Remove") }
                            }
                        } else {
                            OutlinedButton(onClick = { pickFeatured = true }) {
                                Icon(Icons.Filled.AddPhotoAlternate, null); Text(" Set featured image")
                            }
                        }
                    }

                    // ---------- Categories, tags, custom taxonomies ----------
                    type.taxonomies.forEach { tax -> TaxonomyCard(vm, tax) }

                    // ---------- Advanced / custom fields (Phase 2) ----------
                    SectionCard("Advanced") {
                        Text(
                            "Custom fields for this ${type.singularName.lowercase()} will appear here in the next update.",
                            color = DkMuted, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                // ---------- Save bar ----------
                Surface(tonalElevation = 3.dp) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val live = vm.status == "publish" || vm.status == "private"
                        if (!live) OutlinedButton(onClick = { vm.save("draft") }, enabled = !vm.saving) { Text("Save draft") }
                        if (type.can.publish) {
                            IconButton(onClick = { scheduling = true }, enabled = !vm.saving) { Icon(Icons.Filled.Schedule, "Schedule") }
                        }
                        Button(
                            onClick = {
                                when {
                                    !type.can.publish -> vm.save("pending")
                                    vm.status == "future" -> vm.save("future", vm.scheduledAt)
                                    else -> vm.save(if (vm.status == "private") "private" else "publish")
                                }
                            },
                            enabled = !vm.saving,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                when {
                                    !type.can.publish -> "Submit for review"
                                    vm.status == "future" -> "Update schedule"
                                    live -> "Update"
                                    else -> "Publish"
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (pickFeatured) MediaPickerDialog(
        title = "Choose featured image",
        onPick = { vm.featuredId = it.id; vm.featuredUrl = it.thumbUrl; pickFeatured = false },
        onDismiss = { pickFeatured = false },
    )
    if (scheduling) ScheduleDialog(
        initial = vm.scheduledAt,
        onPicked = { scheduling = false; vm.save("future", it) },
        onDismiss = { scheduling = false },
    )
    if (askLeave) ConfirmDialog(
        title = "Discard changes?",
        text = "You have changes that haven't been saved.",
        confirmLabel = "Discard", destructive = true,
        onConfirm = { askLeave = false; onClose() },
        onDismiss = { askLeave = false },
    )
    if (askTrash) ConfirmDialog(
        title = "Move to trash?",
        text = "It will be removed from the website. You can restore it from the trash in WordPress for 30 days.",
        confirmLabel = "Move to trash", destructive = true,
        onConfirm = { askTrash = false; vm.trash() },
        onDismiss = { askTrash = false },
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, Modifier.padding(bottom = 12.dp), style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ElementorNotice(isFrontPage: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(Modifier.padding(16.dp)) {
            Icon(Icons.Filled.Info, null, tint = DkBlue)
            Column(Modifier.padding(start = 12.dp)) {
                Text("Built with Elementor", style = MaterialTheme.typography.titleSmall)
                Text(
                    "This page's layout is edited in Elementor, so its content can't be changed here. " +
                        "You can still change the title, status and featured image." +
                        if (isFrontPage) " The homepage text and images come from the Front Page fields, which you'll be able to edit in the next update." else "",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** Mobile-friendly HTML editor: formatting buttons, image insert, and a live preview. */
@Composable
private fun HtmlEditor(vm: ContentEditViewModel, siteUrl: String) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var pickImage by remember { mutableStateOf(false) }
    var askLink by remember { mutableStateOf(false) }

    TabRow(selectedTabIndex = tab) {
        Tab(tab == 0, { tab = 0 }, text = { Text("Write") })
        Tab(tab == 1, { tab = 1 }, text = { Text("Preview") })
    }
    if (tab == 0) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ToolButton("B") { vm.content = vm.content.wrap("<strong>", "</strong>") }
            ToolButton("I") { vm.content = vm.content.wrap("<em>", "</em>") }
            ToolButton("H2") { vm.content = vm.content.wrap("<h2>", "</h2>") }
            ToolButton("H3") { vm.content = vm.content.wrap("<h3>", "</h3>") }
            ToolButton("¶") { vm.content = vm.content.wrap("<p>", "</p>") }
            ToolButton("• List") { vm.content = vm.content.toList() }
            ToolButton("Link") { askLink = true }
            ToolButton("Image") { pickImage = true }
        }
        OutlinedTextField(
            vm.content, { vm.content = it },
            Modifier.fillMaxWidth().heightIn(min = 240.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            placeholder = { Text("Write your content here…") },
        )
    } else {
        val html = vm.content.text
        AndroidView(
            factory = { ctx -> WebView(ctx).apply { settings.javaScriptEnabled = false; setBackgroundColor(0) } },
            update = { it.loadDataWithBaseURL(siteUrl, previewHtml(html), "text/html", "utf-8", null) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp).padding(top = 8.dp),
        )
    }

    if (pickImage) MediaPickerDialog(
        title = "Insert image",
        onPick = { m ->
            val alt = m.altText.replace("\"", "&quot;")
            vm.content = vm.content.insert("\n<img class=\"wp-image-${m.id}\" src=\"${m.sourceUrl}\" alt=\"$alt\" />\n")
            pickImage = false
        },
        onDismiss = { pickImage = false },
    )
    if (askLink) LinkDialog(onDone = { url -> askLink = false; if (url != null) vm.content = vm.content.wrap("<a href=\"$url\">", "</a>") })
}

@Composable
private fun ToolButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)) { Text(label) }
}

@Composable
private fun LinkDialog(onDone: (String?) -> Unit) {
    var url by remember { mutableStateOf("https://") }
    AlertDialog(
        onDismissRequest = { onDone(null) },
        title = { Text("Add link") },
        text = {
            OutlinedTextField(
                url, { url = it }, singleLine = true, label = { Text("Web address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val ok = url.startsWith("https://") || url.startsWith("http://") || url.startsWith("mailto:") || url.startsWith("tel:") || url.startsWith("/") || url.startsWith("#")
                onDone(if (ok) url.replace("\"", "%22") else null)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = { onDone(null) }) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaxonomyCard(vm: ContentEditViewModel, tax: TaxonomyDto) {
    val options = vm.termOptions[tax.restBase].orEmpty()
    val selected = vm.selectedTerms[tax.restBase].orEmpty()
    var newName by remember { mutableStateOf("") }
    SectionCard(tax.name) {
        if (tax.hierarchical) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { t ->
                    FilterChip(selected = t.id in selected, onClick = { vm.toggleTerm(tax.restBase, t.id) }, label = { Text(t.name) })
                }
            }
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.filter { it.id in selected }.forEach { t ->
                    InputChip(selected = true, onClick = { vm.toggleTerm(tax.restBase, t.id) }, label = { Text(t.name) },
                        trailingIcon = { Text("×") })
                }
            }
        }
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                newName, { newName = it }, Modifier.weight(1f), singleLine = true,
                placeholder = { Text("Add ${if (tax.hierarchical) "new" else ""} ${tax.name.lowercase().removeSuffix("s")}") },
            )
            TextButton(onClick = { vm.addTerm(tax.restBase, newName); newName = "" }, enabled = newName.isNotBlank()) { Text("Add") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDialog(initial: LocalDateTime?, onPicked: (LocalDateTime) -> Unit, onDismiss: () -> Unit) {
    val start = initial ?: LocalDateTime.now().plusDays(1).withHour(9).withMinute(0)
    val date = rememberDatePickerState(initialSelectedDateMillis = start.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    val time = rememberTimePickerState(initialHour = start.hour, initialMinute = start.minute, is24Hour = true)
    var step by remember { mutableStateOf(0) }

    if (step == 0) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = { step = 1 }, enabled = date.selectedDateMillis != null) { Text("Next") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        ) { DatePicker(date) }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Publish at") },
            text = { Box(Modifier.widthIn(max = 360.dp)) { TimePicker(time) } },
            confirmButton = {
                TextButton(onClick = {
                    date.selectedDateMillis?.let { ms ->
                        val day = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        onPicked(day.atTime(time.hour, time.minute))
                    }
                }) { Text("Schedule") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        )
    }
}

// ---------- small text helpers ----------

private fun TextFieldValue.wrap(open: String, close: String): TextFieldValue {
    val s = selection.min; val e = selection.max
    val newText = text.substring(0, s) + open + text.substring(s, e) + close + text.substring(e)
    val cursor = if (s == e) s + open.length else e + open.length + close.length
    return copy(text = newText, selection = TextRange(cursor))
}

private fun TextFieldValue.insert(snippet: String): TextFieldValue {
    val s = selection.min
    return copy(text = text.substring(0, s) + snippet + text.substring(selection.max), selection = TextRange(s + snippet.length))
}

private fun TextFieldValue.toList(): TextFieldValue {
    val s = selection.min; val e = selection.max
    val lines = text.substring(s, e).lines().filter { it.isNotBlank() }.ifEmpty { listOf("") }
    val block = "<ul>\n" + lines.joinToString("\n") { "  <li>${it.trim()}</li>" } + "\n</ul>"
    return copy(text = text.substring(0, s) + block + text.substring(e), selection = TextRange(s + block.length))
}

private fun previewHtml(body: String) = """
<!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1">
<style>
body{font-family:sans-serif;color:#f3f5f7;background:transparent;line-height:1.6;margin:0;padding:4px}
img{max-width:100%;height:auto}a{color:#00a8ff}h1,h2,h3{line-height:1.2}
</style></head><body>${body.replace("\n\n", "<br><br>")}</body></html>
""".trimIndent()
