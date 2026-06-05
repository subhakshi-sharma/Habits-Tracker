package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Light Energetic Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = EnergeticPrimary,
    onPrimary = EnergeticOnPrimary,
    primaryContainer = EnergeticPrimaryContainer,
    onPrimaryContainer = EnergeticOnPrimaryContainer,
    secondary = EnergeticSecondary,
    onSecondary = EnergeticOnPrimary,
    secondaryContainer = EnergeticSecondaryContainer,
    background = EnergeticBg,
    onBackground = EnergeticTextDark,
    surface = EnergeticSurface,
    onSurface = EnergeticTextDark,
    surfaceVariant = EnergeticSurfaceVariant,
    onSurfaceVariant = EnergeticTextGray,
    outline = EnergeticBorder,
    outlineVariant = EnergeticBorder
)

// Dark Energetic style
private val DarkColorScheme = darkColorScheme(
    primary = EnergeticPrimary,
    onPrimary = EnergeticOnPrimary,
    primaryContainer = EnergeticOnPrimaryContainer,
    onPrimaryContainer = EnergeticPrimaryContainer,
    secondary = EnergeticSecondary,
    onSecondary = EnergeticOnPrimary,
    secondaryContainer = Color(0xFF00306F),
    onSecondaryContainer = EnergeticSecondaryContainer,
    background = Color(0xFF121212),
    onBackground = EnergeticBg,
    surface = Color(0xFF1E1E24),
    onSurface = EnergeticBg,
    surfaceVariant = Color(0xFF2C2C35),
    onSurfaceVariant = Color(0xFFE0E0E0),
    outline = Color(0x66FFFFFF),
    outlineVariant = Color(0x33FFFFFF)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set to false by default to preserve the custom "Clean Minimalism" identity precisely
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
