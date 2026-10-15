package com.alamal.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.alamal.app.R

/**
 * Arabic typography for Alamal — Tajawal.
 *
 * Why Tajawal, not Cairo?
 *   Cairo ships as a variable font (one file, weight axis 200-1000).
 *   Android renders variable fonts inconsistently across devices and
 *   Compose versions, which produced the "low resolution" / mixed-glyph
 *   look you saw. Tajawal ships as separate static files, so every weight
 *   loads cleanly and identically on every device.
 *
 * Why no letterSpacing?
 *   Arabic is a cursive script: letters join (كتب). Applying letter
 *   spacing forces them apart visually and breaks the word shape. All
 *   Arabic typography must keep letterSpacing at the default 0.
 *
 * SemiBold handling:
 *   Tajawal has no SemiBold (600). We declare the Bold file twice, once
 *   tagged 600 and once tagged 700, so any code requesting SemiBold
 *   resolves to Bold instead of falling back to the system font.
 */

@Suppress("DEPRECATION")
private val NoFontPadding = PlatformTextStyle(includeFontPadding = false)

private val ArabicLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

val Tajawal = FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_medium, FontWeight.Medium),
    Font(R.font.tajawal_bold, FontWeight.SemiBold),
    Font(R.font.tajawal_bold, FontWeight.Bold),
)

private fun arabic(
    size: Int,
    line: Int,
    weight: FontWeight = FontWeight.Normal,
): TextStyle = TextStyle(
    fontFamily = Tajawal,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    platformStyle = NoFontPadding,
    lineHeightStyle = ArabicLineHeight,
)

/**
 * Sizes tuned for Arabic on a phone screen.
 * Line heights ~1.45-1.55x the font size keep Arabic's tall vertical
 * extent readable without making the layout feel airy.
 */
val AlamalTypography = Typography(
    displayLarge = arabic(48, 58, FontWeight.Medium),
    displayMedium = arabic(40, 48, FontWeight.Medium),
    displaySmall = arabic(34, 42, FontWeight.Medium),

    headlineLarge = arabic(30, 40, FontWeight.Medium),
    headlineMedium = arabic(26, 34, FontWeight.Medium),
    headlineSmall = arabic(24, 32, FontWeight.Medium),

    titleLarge = arabic(21, 28, FontWeight.Medium),
    titleMedium = arabic(18, 26, FontWeight.Medium),
    titleSmall = arabic(16, 23, FontWeight.Medium),

    bodyLarge = arabic(17, 26, FontWeight.Normal),
    bodyMedium = arabic(16, 24, FontWeight.Normal),
    bodySmall = arabic(14, 21, FontWeight.Normal),

    labelLarge = arabic(15, 21, FontWeight.Medium),
    labelMedium = arabic(14, 20, FontWeight.Medium),
    labelSmall = arabic(12, 18, FontWeight.Medium),
)
