package com.alamal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.alamal.app.data.prefs.ThemeMode

private fun Color.tint(amount: Float): Color = lerp(this, Color.White, amount)
private fun Color.shade(amount: Float): Color = lerp(this, Color.Black, amount)

private fun lightSchemeFrom(seed: Color) = lightColorScheme(
    primary = seed.shade(0.15f),
    onPrimary = Color.White,
    primaryContainer = seed.tint(0.78f),
    onPrimaryContainer = seed.shade(0.55f),

    secondary = seed.shade(0.30f),
    onSecondary = Color.White,
    secondaryContainer = seed.tint(0.78f),
    onSecondaryContainer = seed.shade(0.55f),

    tertiary = seed.shade(0.40f),
    onTertiary = Color.White,
    tertiaryContainer = seed.tint(0.78f),
    onTertiaryContainer = seed.shade(0.58f),

    surfaceTint = seed.shade(0.15f),
    inversePrimary = seed.tint(0.40f),
)

private fun darkSchemeFrom(seed: Color) = darkColorScheme(
    primary = seed.tint(0.35f),
    onPrimary = seed.shade(0.75f),
    primaryContainer = seed.shade(0.40f),
    onPrimaryContainer = seed.tint(0.85f),

    secondary = seed.tint(0.45f),
    onSecondary = seed.shade(0.75f),
    secondaryContainer = seed.shade(0.40f),
    onSecondaryContainer = seed.tint(0.85f),

    tertiary = seed.tint(0.50f),
    onTertiary = seed.shade(0.75f),
    tertiaryContainer = seed.shade(0.45f),
    onTertiaryContainer = seed.tint(0.85f),

    surfaceTint = seed.tint(0.35f),
    inversePrimary = seed.shade(0.15f),
)

@Composable
fun AlamalTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    seedColor: Color = SeedBlue,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (dark) darkSchemeFrom(seedColor) else lightSchemeFrom(seedColor)
    MaterialTheme(
        colorScheme = scheme,
        typography = AlamalTypography,
        content = content,
    )
}

/**
 * Parses "#RRGGBB" or "#AARRGGBB". Falls back to a blue if invalid.
 */
fun parseHexColor(hex: String, fallback: Color = SeedBlue): Color {
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
