package com.stoneshield.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LightSafe = Color(0xFF43A047)
val LightWarn = Color(0xFFFFA726)
val LightDanger = Color(0xFFD32F2F)
val LightBlue = Color(0xFF1565C0)
val DarkSafe = Color(0xFFA5D6A7)
val DarkWarn = Color(0xFFFFE082)
val DarkDanger = Color(0xFFEF9A9A)
val DarkBlue = Color(0xFF90CAF9)

private val LightColorScheme = lightColorScheme(
    primary = LightBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    secondary = LightSafe,
    tertiary = LightWarn,
    error = LightDanger,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkBlue,
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1565C0),
    secondary = DarkSafe,
    tertiary = DarkWarn,
    error = DarkDanger,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
)

@Composable
fun StoneShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
