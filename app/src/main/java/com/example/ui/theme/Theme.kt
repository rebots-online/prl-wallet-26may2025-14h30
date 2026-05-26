package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ObsidianColorScheme = darkColorScheme(
    primary = CyanPearl,
    onPrimary = ObsidianDeep,
    primaryContainer = CyanPearl,
    onPrimaryContainer = ObsidianDeep,
    secondary = SecondaryGold,
    onSecondary = ObsidianDeep,
    secondaryContainer = AccentYellow,
    onSecondaryContainer = ObsidianDeep,
    tertiary = AccentGreen,
    onTertiary = ObsidianDeep,
    background = ObsidianDeep,
    onBackground = OnSurface,
    surface = DeepObsidian,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainer,
    onSurfaceVariant = OnSurfaceVariant,
    outline = OutlineColors,
    outlineVariant = OutlineVariantColors,
    error = ErrorRed,
    onError = OnErrorDark,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

@Composable
fun PearlTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ObsidianColorScheme,
        typography = Typography,
        content = content
    )
}
