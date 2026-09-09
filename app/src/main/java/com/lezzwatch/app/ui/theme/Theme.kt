package com.lezzwatch.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.lezzwatch.app.data.local.prefs.AppTheme

private val LezzwatchDarkColors = darkColorScheme(
    primary = AccentPurple,
    onPrimary = DarkOnBackground,
    secondary = AccentPurpleLight,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceMuted,
    outline = DarkOutline,
    error = ErrorRed,
)

private val LezzwatchLightColors = lightColorScheme(
    primary = AccentPurple,
    onPrimary = LightSurface,
    secondary = AccentPurpleLight,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceMuted,
    outline = LightOutline,
    error = ErrorRed,
)

/**
 * App theme wrapper. Lezzwatch defaults to dark (this is a video/TV app), but honors the user's
 * explicit Light or System choice from Settings.
 */
@Composable
fun LezzwatchTheme(
    appTheme: AppTheme = AppTheme.DARK,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (appTheme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDarkTheme) LezzwatchDarkColors else LezzwatchLightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LezzwatchTypography,
        content = content,
    )
}
