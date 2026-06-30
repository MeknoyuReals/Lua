package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CyberColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = CyberBlack,
    secondary = CyberTeal,
    onSecondary = CyberBlack,
    tertiary = CyberMagenta,
    onTertiary = CyberBlack,
    background = CyberBlack,
    onBackground = CyberWhiteText,
    surface = CyberDarkCard,
    onSurface = CyberWhiteText,
    surfaceVariant = CyberDarkSurface,
    onSurfaceVariant = CyberWhiteText,
    outline = CyberMutedPurple
)

@Composable
fun MeknoyuTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberColorScheme,
        typography = Typography,
        content = content
    )
}
