package se.duokraft.admin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Duokraft brand colours (same as the website's global colours).
val DkBlue = Color(0xFF00A8FF)
val DkBackground = Color(0xFF040C12)
val DkSurface = Color(0xFF0A1722)
val DkSurfaceHigh = Color(0xFF102231)
val DkText = Color(0xFFF3F5F7)
val DkMuted = Color(0xFFC5CBD1)
val DkLine = Color(0xFF1C3444)
val DkSuccess = Color(0xFF3DDC97)
val DkWarning = Color(0xFFFFC857)
val DkDanger = Color(0xFFFF6B6B)

private val Colors = darkColorScheme(
    primary = DkBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF053049),
    onPrimaryContainer = Color(0xFFBFE8FF),
    secondary = Color(0xFF80D5FF),
    background = DkBackground,
    onBackground = DkText,
    surface = DkSurface,
    onSurface = DkText,
    surfaceVariant = DkSurfaceHigh,
    onSurfaceVariant = DkMuted,
    surfaceContainer = DkSurface,
    surfaceContainerHigh = DkSurfaceHigh,
    surfaceContainerHighest = Color(0xFF16303F),
    outline = DkLine,
    outlineVariant = Color(0xFF142630),
    error = DkDanger,
)

@Composable
fun DuokraftTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
