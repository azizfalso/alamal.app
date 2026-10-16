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
    outline = BorderMediumLight,
    outlineVariant = BorderSoftLight,
    scrim = NearBlack,
)

private fun darkSchemeFrom(seed: Color) = darkColorScheme(
    // Dark-theme accent uses the explicitly tuned GoldOliveBright instead
    // of tinting the seed, because tinting a warm hue toward white produces
    // a muddy grey that doesn't match the app's warm identity.
    primary = GoldOliveBright,
    onPrimary = NearBlack,
    primaryContainer = GoldOlive,
    onPrimaryContainer = DarkTextMuted,
    secondary = GoldOliveLight,
    onSecondary = NearBlack,
    secondaryContainer = GoldOlive,
    onSecondaryContainer = DarkTextMuted,
    tertiary = GoldOliveLight,
    onTertiary = NearBlack,
    tertiaryContainer = GoldOlive,
    onTertiaryContainer = DarkTextMuted,
    error = Color(0xFFD88A8A),
    onError = NearBlack,
    errorContainer = Color(0xFF4A2828),
    onErrorContainer = Color(0xFFFFDAD6),
    background = DarkDeep,
    onBackground = Ivory,
    surface = DarkDeep,
    onSurface = Ivory,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkTextMuted,
    surfaceTint = GoldOliveBright,
    inverseSurface = Parchment,
    inverseOnSurface = NearBlack,
    inversePrimary = GoldOlive,
    surfaceContainerLowest = DarkDeep,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkElevated,
    surfaceContainerHighest = DarkElevated,
    outline = BorderMediumDark,
    outlineVariant = BorderSoftDark,
    scrim = Color(0xFF000000),
)

private fun extendedFrom(seed: Color, dark: Boolean): ExtendedColors =
    if (dark) {
        ExtendedColors(
            muted = DarkTextMuted,
            meta = DarkTextMeta,
            border = BorderSoftDark,
            borderStrong = BorderMediumDark,
            ring = BorderMediumDark,
            ringPressed = Color(0xFF6A6965),
            accent = GoldOliveBright,
            accentPressed = GoldOliveLight,
            accentOn = NearBlack,
            onWarmSurface = WarmSilver,
            success = SuccessGreen,
            warn = WarnGold,
            accentSurface = AccentSurfaceDark,
            accentSurfaceBorder = AccentSurfaceBorderDark,
            workingSurface = WorkingSurfaceDark,
            workingBorder = WorkingBorder,
            workingText = WorkingTextDark,
        )
    } else {
        ExtendedColors(
            muted = OliveGray,
            meta = StoneGray,
            border = BorderSoftLight,
            borderStrong = BorderMediumLight,
            ring = BorderMediumLight,
            ringPressed = RingDeep,
            accent = seed,
            accentPressed = seed.shade(0.10f),
            accentOn = Ivory,
            onWarmSurface = CharcoalWarm,
            success = SuccessGreen,
            warn = WarnGold,
            accentSurface = AccentSurfaceLight,
            accentSurfaceBorder = AccentSurfaceBorderLight,
            workingSurface = WorkingSurfaceLight,
            workingBorder = WorkingBorder,
            workingText = WorkingTextLight,
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
