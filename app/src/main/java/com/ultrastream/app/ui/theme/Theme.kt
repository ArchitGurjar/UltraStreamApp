package com.ultrastream.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentPurple,
    tertiary = AccentGold,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = TextMainDark,
    onSecondary = TextMainDark,
    onBackground = TextMainDark,
    onSurface = TextMainDark
)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    secondary = AccentPurple,
    tertiary = AccentGold,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = TextMainLight,
    onSecondary = TextMainLight,
    onBackground = TextMainLight,
    onSurface = TextMainLight
)

@Composable
fun UltraStreamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
