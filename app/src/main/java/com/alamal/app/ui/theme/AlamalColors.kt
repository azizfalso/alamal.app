package com.alamal.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extended color roles not present in Material 3 ColorScheme.
 * Exposes Claude-style semantic colors (muted, meta, border, ring, accent).
 *
 * Access with: LocalExtendedColors.current.muted
 */
data class ExtendedColors(
    val muted: Color,
    val meta: Color,
    val border: Color,
    val borderStrong: Color,
    val ring: Color,
    val ringPressed: Color,
    val accent: Color,
    val accentPressed: Color,
    val accentOn: Color,
    val onWarmSurface: Color,
    val success: Color,
    val warn: Color,
)

val LightExtendedColors = ExtendedColors(
    muted = OliveGray,
    meta = StoneGray,
    border = BorderCream,
    borderStrong = BorderWarm,
    ring = RingWarm,
    ringPressed = RingDeep,
    accent = Terracotta,
    accentPressed = TerracottaHover,
    accentOn = Ivory,
    onWarmSurface = CharcoalWarm,
    success = SuccessGreen,
    warn = WarnGold,
)

val DarkExtendedColors = ExtendedColors(
    muted = WarmSilver,
    meta = Color(0xFF87867F),
    border = DarkSurface,
    borderStrong = DarkElevated,
    ring = DarkElevated,
    ringPressed = Color(0xFF4D4C48),
    accent = Coral,
    accentPressed = Terracotta,
    accentOn = NearBlack,
    onWarmSurface = WarmSilver,
    success = SuccessGreen,
    warn = WarnGold,
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
