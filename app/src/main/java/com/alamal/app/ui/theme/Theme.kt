package com.alamal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
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

    error = ErrorCrimson,
    onError = Ivory,
    errorContainer = Color(0xFFF2D9D9),
    onErrorContainer = ErrorCrimson,

    background = Parchment,
    onBackground = NearBlack,
    surface = Parchment,
    onSurface = NearBlack,
    surfaceVariant = WarmSand,
    onSurfaceVariant = OliveGray,
    surfaceTint = seed.shade(0.15f),
    inverseSurface = DarkSurface,
    inverseOnSurface = Ivory,
    inversePrimary = seed.tint(0.40f),

    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = Ivory,
    surfaceContainer = Ivory,
    surfaceContainerHigh = WarmSand,
    surfaceContainerHighest = WarmSand,

    outline = RingWarm,
    outlineVariant = BorderCream,

    scrim = NearBlack,
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
    surfaceTint = seed.tint(0.35f),
    inverseSurface = Parchment,
    inverseOnSurface = NearBlack,
    inversePrimary = seed.shade(0.15f),

    surfaceContainerLowest = DarkDeep,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkElevated,
    surfaceContainerHighest = DarkElevated,

    outline = DarkElevated,
    outlineVariant = DarkSurface,

    scrim = Color(0xFF000000),
)

/**
 * Builds the extended Claude-style colors from the user's chosen seed color.
 * The accent follows the seed so the whole UI changes when the user
 * picks a new color in Settings.
 */
private fun extendedFrom(seed: Color, dark: Boolean): ExtendedColors =
    if (dark) {
        ExtendedColors(
            muted = WarmSilver,
            meta = Color(0xFF87867F),
            border = DarkSurface,
            borderStrong = DarkElevated,
            ring = DarkElevated,
            ringPressed = Color(0xFF4D4C48),
            accent = seed.tint(0.30f),
            accentPressed = seed.tint(0.10f),
            accentOn = NearBlack,
            onWarmSurface = WarmSilver,
            success = SuccessGreen,
            warn = WarnGold,
        )
    } else {
        ExtendedColors(
            muted = OliveGray,
            meta = StoneGray,
            border = BorderCream,
            borderStrong = BorderWarm,
            ring = RingWarm,
            ringPressed = RingDeep,
            accent = seed,
            accentPressed = seed.shade(0.10f),
            accentOn = Ivory,
            onWarmSurface = CharcoalWarm,
            success = SuccessGreen,
            warn = WarnGold,
        )
    }

@Composable
fun AlamalTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    seedColor: Color = GoldOlive,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (dark) darkSchemeFrom(seedColor) else lightSchemeFrom(seedColor)
    val extended = remember(seedColor, dark) { extendedFrom(seedColor, dark) }

    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AlamalTypography,
            shapes = AlamalShapes,
            content = content,
        )
    }
}

fun parseHexColor(hex: String, fallback: Color = GoldOlive): Color {
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
