package com.cardforge.studio.ui.theme

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

private val DarkScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = AccentBlueDark,
    onPrimaryContainer = Color.White,
    secondary = AccentCyan,
    onSecondary = NavyDeep,
    background = NavyDeep,
    onBackground = Color(0xFFE6EDF7),
    surface = NavySurface,
    onSurface = Color(0xFFE6EDF7),
    surfaceVariant = NavyElevated,
    onSurfaceVariant = Color(0xFFB7C6DE),
    outline = NavyOutline,
    error = ErrorRed,
    onError = Color.White
)

private val LightScheme = lightColorScheme(
    primary = AccentBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = NavyDeep,
    secondary = AccentBlue,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightElevated,
    onSurfaceVariant = Color(0xFF4A5568),
    outline = LightOutline,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun CardForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = CardForgeTypography,
        content = content
    )
}
