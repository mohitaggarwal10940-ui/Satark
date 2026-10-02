package com.dev.satark.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ShieldBlueLight,
    onPrimary = Color.White,
    primaryContainer = Slate800,
    onPrimaryContainer = Color.White,
    secondary = ShieldCyan,
    onSecondary = Slate950,
    background = Slate950,
    onBackground = TextPrimaryDark,
    surface = Slate900,
    onSurface = TextPrimaryDark,
    surfaceVariant = Slate850,
    onSurfaceVariant = TextSecondaryDark,
    outline = Slate700,
    outlineVariant = Slate800,
    error = RiskVeryHigh,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ShieldBlue,
    onPrimary = Color.White,
    primaryContainer = Slate100,
    onPrimaryContainer = Slate900,
    secondary = ShieldBlueDark,
    onSecondary = Color.White,
    background = Slate50,
    onBackground = TextPrimaryLight,
    surface = Color.White,
    onSurface = TextPrimaryLight,
    surfaceVariant = Slate100,
    onSurfaceVariant = TextSecondaryLight,
    outline = Slate300,
    outlineVariant = Slate200,
    error = RiskVeryHigh,
    onError = Color.White
)

@Composable
fun SatarkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}