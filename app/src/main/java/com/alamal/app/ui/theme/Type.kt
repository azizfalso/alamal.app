package com.alamal.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.alamal.app.R

/**
 * Arabic typography for Alamal.
 *
 * Why Cairo?
 *   Roboto (the previous FontFamily.Default) has weak Arabic glyphs:
 *   thin strokes, inconsistent spacing, and a Latin-only design that
 *   leaves Arabic text looking "default". Cairo is a modern, neutral
 *   Arabic-first sans with a full weight axis (200-1000) — perfect for
 *   UI headings, body text, tables, and numbers.
 *
 * Variable-font weights:
 *   The .ttf we ship is the variable version (one file, multiple weights).
 *   Android does NOT automatically pick the right instance from
 *   `Font(weight = ...)` alone, so we explicitly request each weight via
 *   FontVariation.Settings. This is the same mechanism the system uses
 *   for variable fonts since API 26 (minSdk here is 32, so it's safe).
 *
 *   FontVariation and the "variable font" plumbing are still marked
 *   @ExperimentalTextApi in Compose. We opt in explicitly below; the
 *   behavior is stable and this is the only supported way to select a
 *   weight from a variable font file.
 *
 * Why the LineHeightStyle / includeFontPadding changes?
 *   Android adds implicit padding around every glyph based on Latin
 *   font metrics. With Arabic this creates a "text floating on a white
 *   page" effect: too much space above/below each line. Disabling that
 *   padding + centering the line height gives text that hugs its
 *   container correctly.
 */
@OptIn(ExperimentalTextApi::class)
val Cairo = FontFamily(
    Font(
        resId = R.font.cairo,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        resId = R.font.cairo,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        resId = R.font.cairo,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        resId = R.font.cairo,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

@Suppress("DEPRECATION")
private val NoFontPadding = PlatformTextStyle(includeFontPadding = false)

/**
 * Center the text inside the line box and trim the extra leading that
 * Arabic glyphs inherit from Latin metrics. If you ever notice clipping
 * on letters like ج، ح، خ، ع، غ (their descenders), change `trim` to
 * LineHeightStyle.Trim.None.
 */
@OptIn(ExperimentalTextApi::class)
private val ArabicLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

@OptIn(ExperimentalTextApi::class)
private fun arabic(
    size: Int,
    line: Int,
    weight: FontWeight = FontWeight.Normal,
): TextStyle = TextStyle(
    fontFamily = Cairo,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    platformStyle = NoFontPadding,
    lineHeightStyle = ArabicLineHeight,
)

/**
 * Sizes are tuned for Arabic: slightly larger than the previous Roboto
 * values and paired with line heights that respect Arabic's taller
 * vertical extent while staying visually compact.
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
