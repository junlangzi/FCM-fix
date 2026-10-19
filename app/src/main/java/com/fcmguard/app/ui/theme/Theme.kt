package com.fcmguard.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val PurplePrimary = Color(0xFF9B59FF)
val PurpleDark = Color(0xFF6B2FCC)
val PurpleLight = Color(0xFFC39BFF)
val GreenSuccess = Color(0xFF00C853)
val RedError = Color(0xFFFF5252)
val BackgroundDark = Color(0xFF0F0F12)
val CardDark = Color(0xFF1C1C22)
val SurfaceDark = Color(0xFF16161A)

// Gradient radiant cho chữ "FCM fix"
val RadiantBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF7C4DFF),
        Color(0xFFB388FF),
        Color(0xFF00E5FF),
        Color(0xFF9B59FF),
        Color(0xFFE040FB)
    )
)

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    secondary = PurpleLight,
    tertiary = GreenSuccess,
    background = BackgroundDark,
    surface = CardDark,
    surfaceVariant = Color(0xFF252530),
    onBackground = Color(0xFFF0F0F5),
    onSurface = Color(0xFFF0F0F5),
    onSurfaceVariant = Color(0xFFB8B8C8),
    error = RedError,
    outline = Color(0xFF3A3A48)
)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    secondary = PurpleDark,
    tertiary = GreenSuccess,
    background = Color(0xFFF7F7FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEEEF5),
    onBackground = Color(0xFF121218),
    onSurface = Color(0xFF121218),
    onSurfaceVariant = Color(0xFF555566),
    error = RedError,
    outline = Color(0xFFD0D0DC)
)

/**
 * @param themeMode SYSTEM | LIGHT | DARK
 */
@Composable
fun FCMGuardTheme(
    themeMode: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> systemDark
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
