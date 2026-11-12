package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyanLight,
    onPrimary = CyberBlack,
    primaryContainer = NeonCyanDark,
    onPrimaryContainer = Color.White,
    secondary = NeonEmerald,
    onSecondary = CyberBlack,
    secondaryContainer = CyberSurfaceVariant,
    onSecondaryContainer = NeonEmeraldLight,
    tertiary = WarningAmber,
    onTertiary = CyberBlack,
    background = CyberBlack,
    onBackground = TextPrimary,
    surface = CyberDark,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurface,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
    outlineVariant = BorderHighlight,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We enforce our sleek dark cybernetic theme for developer/terminal comfort
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
