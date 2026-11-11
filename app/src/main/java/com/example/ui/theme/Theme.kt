package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.SarcasmManager

private val AcidColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF), // Elegant Turquoise
    secondary = Color(0xFFFF4081), // Chic Pink
    tertiary = Color(0xFF80DEEA), // Soft Pastel Cyan
    background = Color(0xFF0E1217), // Deep Slate Dark
    surface = Color(0xFF161B22), // Modern Dark Slate Surface
    surfaceVariant = Color(0xFF21262D), // Sleek Card Surface
    onPrimary = Color(0xFF0E1217),
    onSecondary = Color.White,
    onTertiary = Color(0xFF0E1217),
    onBackground = Color(0xFFF0F6FC), // Off-white modern text
    onSurface = Color(0xFFF0F6FC),
    onSurfaceVariant = Color(0xFF8B949E) // Soft grey for descriptions
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFE2E2E2)
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightCard,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF333333)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    sarcasmLevel: SarcasmManager.SarcasmLevel = SarcasmManager.SarcasmLevel.SEM,
    content: @Composable () -> Unit
) {
    val colorScheme = when (sarcasmLevel) {
        SarcasmManager.SarcasmLevel.MUITO -> AcidColorScheme
        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

