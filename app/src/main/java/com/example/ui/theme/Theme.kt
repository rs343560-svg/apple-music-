package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AppleMusicRed,
    onPrimary = Color.White,
    primaryContainer = AppleMusicRedDark,
    onPrimaryContainer = Color.White,
    secondary = ApplePink,
    onSecondary = Color.White,
    background = AppleDarkBackground,
    onBackground = AppleDarkTextPrimary,
    surface = AppleDarkSurface,
    onSurface = AppleDarkTextPrimary,
    surfaceVariant = AppleDarkSurfaceVariant,
    onSurfaceVariant = AppleDarkTextSecondary,
    outline = AppleDarkDivider
)

private val LightColorScheme = lightColorScheme(
    primary = AppleMusicRed,
    onPrimary = Color.White,
    primaryContainer = AppleMusicRedDark,
    onPrimaryContainer = Color.White,
    secondary = ApplePink,
    onSecondary = Color.White,
    background = AppleLightBackground,
    onBackground = AppleLightTextPrimary,
    surface = AppleLightSurface,
    onSurface = AppleLightTextPrimary,
    surfaceVariant = AppleLightSurfaceVariant,
    onSurfaceVariant = AppleLightTextSecondary,
    outline = AppleLightDivider
)

@Composable
fun MyApplicationTheme(
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
