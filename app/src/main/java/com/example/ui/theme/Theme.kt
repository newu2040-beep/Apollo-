package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.model.ApolloTheme

private fun createApolloLightScheme(
    primaryColor: Color,
    bgColor: Color,
    containerColor: Color
) = lightColorScheme(
    primary = primaryColor,
    onPrimary = Color.White,
    primaryContainer = containerColor,
    onPrimaryContainer = primaryColor,
    secondary = Color(0xFF64748B),
    onSecondary = Color.White,
    background = bgColor,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = containerColor.copy(alpha = 0.45f),
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = bgColor,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = containerColor.copy(alpha = 0.25f),
    surfaceContainerHighest = containerColor.copy(alpha = 0.5f),
    surfaceDim = bgColor,
    surfaceBright = LightSurface,
    outline = LightBorder,
    outlineVariant = Color(0xFFE2E8F0),
    scrim = Color.Black
)

private fun createApolloDarkScheme(
    primaryColor: Color,
    darkBg: Color,
    darkSurface: Color,
    containerColor: Color
) = darkColorScheme(
    primary = primaryColor,
    onPrimary = Color.White,
    primaryContainer = primaryColor.copy(alpha = 0.22f),
    onPrimaryContainer = Color.White,
    secondary = DarkTextSecondary,
    onSecondary = Color.White,
    background = darkBg,
    onBackground = DarkTextPrimary,
    surface = darkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = Color(0xFF1E2532),
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainerLowest = darkBg,
    surfaceContainerLow = darkBg,
    surfaceContainer = darkSurface,
    surfaceContainerHigh = Color(0xFF1E2532),
    surfaceContainerHighest = Color(0xFF28303F),
    surfaceDim = darkBg,
    surfaceBright = darkSurface,
    inverseSurface = LightSurface,
    inverseOnSurface = LightTextPrimary,
    inversePrimary = primaryColor,
    outline = DarkBorder,
    outlineVariant = Color(0xFF2B3342),
    scrim = Color.Black
)

@Composable
fun ApolloAppTheme(
    activeTheme: ApolloTheme = ApolloTheme.SKY,
    forceDark: Boolean? = null,
    content: @Composable () -> Unit
) {
    val darkTheme = forceDark ?: isSystemInDarkTheme()
    val primaryColor = Color(activeTheme.primaryHex)
    val bgColor = Color(activeTheme.backgroundHex)
    val containerColor = Color(activeTheme.containerHex)
    val darkBg = Color(activeTheme.darkBackgroundHex)
    val darkSurface = Color(activeTheme.darkSurfaceHex)

    val colorScheme = if (darkTheme) {
        createApolloDarkScheme(primaryColor, darkBg, darkSurface, containerColor)
    } else {
        createApolloLightScheme(primaryColor, bgColor, containerColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
