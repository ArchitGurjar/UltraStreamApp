package com.ultrastream.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = TextMainDark,
    secondary = AccentPurple,
    onSecondary = TextMainDark,
    tertiary = AccentGold,
    onTertiary = TextMainDark,
    background = BackgroundDark,
    onBackground = TextMainDark,
    surface = SurfaceDark,
    onSurface = TextMainDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextMutedDark,
    error = AccentRed,
    onError = TextMainDark
)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = TextMainLight,
    secondary = AccentPurple,
    onSecondary = TextMainLight,
    tertiary = AccentGold,
    onTertiary = TextMainLight,
    background = BackgroundLight,
    onBackground = TextMainLight,
    surface = SurfaceLight,
    onSurface = TextMainLight,
    surfaceVariant = CardLight,
    onSurfaceVariant = TextMutedLight,
    error = AccentRed,
    onError = TextMainLight
)

// Local composition for custom colors if needed
val LocalCustomColors = staticCompositionLocalOf { CustomColors() }

data class CustomColors(
    val accentBlue: androidx.compose.ui.graphics.Color = AccentBlue,
    val accentGold: androidx.compose.ui.graphics.Color = AccentGold,
    val accentRed: androidx.compose.ui.graphics.Color = AccentRed,
    val accentGreen: androidx.compose.ui.graphics.Color = AccentGreen,
    val accentPurple: androidx.compose.ui.graphics.Color = AccentPurple,
    val accentPink: androidx.compose.ui.graphics.Color = AccentPink,
    val accentOrange: androidx.compose.ui.graphics.Color = AccentOrange,
    val textMuted: androidx.compose.ui.graphics.Color = TextMutedDark
)

@Composable
fun UltraStreamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val customColors = if (darkTheme) {
        CustomColors(
            textMuted = TextMutedDark
        )
    } else {
        CustomColors(
            textMuted = TextMutedLight
        )
    }

    CompositionLocalProvider(
        LocalCustomColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
