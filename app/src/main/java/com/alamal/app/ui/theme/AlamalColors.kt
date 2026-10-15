package com.alamal.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extended color roles not present in Material 3 ColorScheme.
 * Exposes Claude-style semantic colors (muted, meta, border, ring, accent).
 *
 * Access with: LocalExtendedColors.current.muted
 *
 * These top-level values are kept as a static fallback. The actual runtime
 * values are built in Theme.kt's `extendedFrom(seed, dark)`, which uses the
 * same tokens defined in Color.kt.
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
    border = BorderSoftLight,
    borderStrong = BorderMediumLight,
    ring = BorderMediumLight,
    ringPressed = RingDeep,
    accent = GoldOlive,
    accentPressed = GoldOliveHover,
    accentOn = Ivory,
    onWarmSurface = CharcoalWarm,
    success = SuccessGreen,
    warn = WarnGold,
)

val DarkExtendedColors = ExtendedColors(
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
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
