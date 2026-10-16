package com.alamal.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extended color roles not present in Material 3 ColorScheme.
 *
 * New in this revision:
 *   - accentSurface / accentSurfaceBorder: for the "clock-in" hero card.
 *     It used to be a solid gold fill; now it's a soft tinted surface with
 *     a warm border, so it still stands out but no longer dominates.
 *   - workingSurface / workingBorder / workingText: for the "at work" card.
 *     Switched from saturated #17A34A to a calmer olive.
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
    // New surface tokens
    val accentSurface: Color,
    val accentSurfaceBorder: Color,
    val workingSurface: Color,
    val workingBorder: Color,
    val workingText: Color,
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
    accentSurface = AccentSurfaceLight,
    accentSurfaceBorder = AccentSurfaceBorderLight,
    workingSurface = WorkingSurfaceLight,
    workingBorder = WorkingBorder,
    workingText = WorkingTextLight,
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
    accentSurface = AccentSurfaceDark,
    accentSurfaceBorder = AccentSurfaceBorderDark,
    workingSurface = WorkingSurfaceDark,
    workingBorder = WorkingBorder,
    workingText = WorkingTextDark,
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
