package se.duokraft.admin.ui.media

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import se.duokraft.admin.data.net.MediaDto
import se.duokraft.admin.ui.appContainer
import se.duokraft.admin.ui.components.SearchField

/** Full-screen picker: choose an existing image or upload a new one from the phone. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerDialog(title: String, onPick: (MediaDto) -> Unit, onDismiss: () -> Unit) {
    val c = appContainer()
    val vm: MediaViewModel = viewModel(key = "media-picker") { MediaViewModel(c) }
    val upload = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.upload(listOf(uri)) { onPick(it) }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close") } },
                    actions = {
                        TextButton(onClick = { upload.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                            Icon(Icons.Filled.Upload, null); Text(" Upload new")
                        }
                    },
                )
                SearchField(vm.search, vm::onSearch, "Search media", Modifier.padding(horizontal = 16.dp))
                if (vm.uploading > 0) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                MediaGrid(vm, onClick = onPick)
            }
        }
    }
}
