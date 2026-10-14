package com.alamal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.alamal.app.data.prefs.ThemeMode

// ---------- Claude-inspired schemes (fixed, no Material You) ----------

private val LightScheme = lightColorScheme(
    // Brand / accent
    primary = Terracotta,
    onPrimary = Ivory,
    primaryContainer = WarmSand,
    onPrimaryContainer = NearBlack,

    secondary = CharcoalWarm,
    onSecondary = Ivory,
    secondaryContainer = WarmSand,
    onSecondaryContainer = NearBlack,

    tertiary = Coral,
    onTertiary = Ivory,
    tertiaryContainer = WarmSand,
    onTertiaryContainer = NearBlack,

    // Errors
    error = ErrorCrimson,
    onError = Ivory,
    errorContainer = Color(0xFFF2D9D9),
    onErrorContainer = ErrorCrimson,

    // Surfaces (light)
    background = Parchment,
    onBackground = NearBlack,
    surface = Parchment,
    onSurface = NearBlack,
    surfaceVariant = WarmSand,
    onSurfaceVariant = OliveGray,
    surfaceTint = Terracotta,
    inverseSurface = DarkSurface,
    inverseOnSurface = Ivory,
    inversePrimary = Coral,

    // Containers (used by Cards)
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = Ivory,
    surfaceContainer = Ivory,
    surfaceContainerHigh = WarmSand,
    surfaceContainerHighest = WarmSand,

    // Borders / outlines
    outline = RingWarm,
    outlineVariant = BorderCream,

    scrim = NearBlack,
)

private val DarkScheme = darkColorScheme(
    primary = Coral,
    onPrimary = NearBlack,
    primaryContainer = DarkSurface,
    onPrimaryContainer = Ivory,

    secondary = WarmSilver,
    onSecondary = NearBlack,
    secondaryContainer = DarkSurface,
    onSecondaryContainer = Ivory,

    tertiary = Terracotta,
    onTertiary = Ivory,
    tertiaryContainer = DarkSurface,
    onTertiaryContainer = Ivory,

    error = Color(0xFFE57373),
    onError = NearBlack,
    errorContainer = Color(0xFF4A2828),
    onErrorContainer = Color(0xFFFFDAD6),

    background = DarkDeep,
    onBackground = Ivory,
    surface = DarkDeep,
    onSurface = Ivory,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = WarmSilver,
    surfaceTint = Coral,
    inverseSurface = Parchment,
    inverseOnSurface = NearBlack,
    inversePrimary = Terracotta,

    surfaceContainerLowest = DarkDeep,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkElevated,
    surfaceContainerHighest = DarkElevated,

    outline = DarkElevated,
    outlineVariant = DarkSurface,

    scrim = Color(0xFF000000),
)

@Composable
fun AlamalTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    @Suppress("UNUSED_PARAMETER") seedColor: Color = Terracotta,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (dark) DarkScheme else LightScheme
    val extended = if (dark) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AlamalTypography,
            shapes = AlamalShapes,
            content = content,
        )
    }
}

// -------- Helpers retained for compatibility with Settings --------

fun parseHexColor(hex: String, fallback: Color = Terracotta): Color {
    val cleaned = hex.trim().removePrefix("#")
    return try {
        val v = cleaned.toLong(16)
        when (cleaned.length) {
            6 -> Color(0xFF000000L or v)
            8 -> Color(v)
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}

fun isHexColorValid(hex: String): Boolean {
    val cleaned = hex.trim().removePrefix("#")
    if (cleaned.length != 6 && cleaned.length != 8) return false
    return cleaned.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
}
