package se.duokraft.admin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import se.duokraft.admin.ui.theme.DkBlue
import se.duokraft.admin.ui.theme.DkMuted
import se.duokraft.admin.ui.theme.DkSuccess
import se.duokraft.admin.ui.theme.DkWarning
import java.time.OffsetDateTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.CloudOff, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
        Text(message, Modifier.padding(vertical = 16.dp), textAlign = TextAlign.Center)
        Button(onClick = onRetry) { Text("Try again") }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, hint: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, Modifier.size(48.dp), tint = DkMuted)
        Text(title, Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (hint != null) Text(hint, Modifier.padding(top = 6.dp), color = DkMuted, textAlign = TextAlign.Center)
    }
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        trailingIcon = {
            if (value.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Filled.Clear, "Clear search") }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
    )
}

/** Human label + colour for a WordPress post status. */
fun statusLabel(status: String): Pair<String, Color> = when (status) {
    "publish" -> "Published" to DkSuccess
    "draft" -> "Draft" to DkMuted
    "pending" -> "Pending" to DkWarning
    "future" -> "Scheduled" to DkBlue
    "private" -> "Private" to Color(0xFFB39DFF)
    "trash" -> "Trash" to Color(0xFFFF6B6B)
    else -> status.replaceFirstChar { it.uppercase() } to DkMuted
}

@Composable
fun StatusBadge(status: String) {
    val (label, color) = statusLabel(status)
    Badge(label, color)
}

@Composable
fun Badge(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
        Text(
            label,
            Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled) { Text(text) }
}

/** "12 Oct 2026, 14:05" from a WordPress date ("2026-10-12T14:05:00" or with offset). */
fun formatWpDate(value: String?): String {
    if (value.isNullOrBlank()) return ""
    val fmt = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    return runCatching { OffsetDateTime.parse(value).toLocalDateTime().format(fmt) }
        .recoverCatching { LocalDateTime.parse(value).format(fmt) }
        .getOrDefault(value)
}

/** Plain text from WordPress' HTML-escaped titles. */
fun wpText(html: String): String = android.text.Html.fromHtml(html, 0).toString().trim()
