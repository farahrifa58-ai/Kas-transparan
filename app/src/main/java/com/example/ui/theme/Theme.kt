package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldGreenPrimaryDark,
    onPrimary = EmeraldGreenOnPrimaryDark,
    primaryContainer = EmeraldGreenPrimaryContainerDark,
    onPrimaryContainer = EmeraldGreenOnPrimaryContainerDark,
    secondary = TealSecondaryDark,
    onSecondary = TealOnSecondaryDark,
    background = AppBackgroundDark,
    surface = AppSurfaceDark,
    onBackground = Color(0xFFE2E3DE),
    onSurface = Color(0xFFE2E3DE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldGreenPrimary,
    onPrimary = EmeraldGreenOnPrimary,
    primaryContainer = EmeraldGreenPrimaryContainer,
    onPrimaryContainer = EmeraldGreenOnPrimaryContainer,
    secondary = TealSecondary,
    onSecondary = TealOnSecondary,
    background = AppBackgroundLight,
    surface = AppSurfaceLight,
    onBackground = Color(0xFF191C19),
    onSurface = Color(0xFF191C19),
    error = ExpenseRed,
    onError = Color.White
)

@Composable
fun KasTransparanTheme(
    themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
