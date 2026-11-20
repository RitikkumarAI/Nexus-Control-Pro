package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

interface ThemeController {
    val currentMode: ThemeMode
    val isDark: Boolean
    fun setThemeMode(mode: ThemeMode)
    fun toggleTheme()
}

val LocalThemeController = compositionLocalOf<ThemeController> {
    object : ThemeController {
        override val currentMode: ThemeMode = ThemeMode.SYSTEM
        override val isDark: Boolean = false
        override fun setThemeMode(mode: ThemeMode) {}
        override fun toggleTheme() {}
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = NexusBluePrimary,
    onPrimary = Color.White,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = NexusCyanAccent,
    secondary = NexusCyanAccent,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkCardBorder,
    outlineVariant = DarkCardBorderSubtle,
    error = CrimsonStopDark,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = NexusBluePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0EDFF),
    onPrimaryContainer = NexusBlueDark,
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightCardBorder,
    outlineVariant = LightCardBorderSubtle,
    error = CrimsonStop,
    onError = Color.White
)

/**
 * Enterprise Theme Provider supporting System, Light, and Dark modes.
 * Provides [LocalThemeController] to allow any dashboard element to observe or switch themes.
 */
@Composable
fun NexusThemeProvider(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChanged: (ThemeMode) -> Unit = {},
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemInDark
    }

    val controller = remember(themeMode, isDark) {
        object : ThemeController {
            override val currentMode: ThemeMode = themeMode
            override val isDark: Boolean = isDark
            override fun setThemeMode(mode: ThemeMode) {
                onThemeModeChanged(mode)
            }
            override fun toggleTheme() {
                val nextMode = when (themeMode) {
                    ThemeMode.DARK -> ThemeMode.LIGHT
                    ThemeMode.LIGHT -> ThemeMode.DARK
                    ThemeMode.SYSTEM -> if (systemInDark) ThemeMode.LIGHT else ThemeMode.DARK
                }
                onThemeModeChanged(nextMode)
            }
        }
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalThemeController provides controller) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun NexusTheme(
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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    NexusTheme(darkTheme = darkTheme, content = content)
}
