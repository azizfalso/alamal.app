package com.alamal.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Claude-inspired type scale.
 *
 * - Headlines: medium weight (500), slightly tight line height.
 * - Body: regular (400), relaxed line height (1.5–1.6) for Arabic readability.
 * - Uses the platform default font family; Android will pick the best
 *   Arabic-capable font (Noto Naskh Arabic or system Arabic).
 *
 * If you want to ship a specific Arabic font later, drop the .ttf in
 * res/font/ and replace `FontFamily.Default` here.
 */
private val Default = FontFamily.Default

val AlamalTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = 44.sp,
        lineHeight = 52.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = 36.sp,
        lineHeight = 44.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 38.sp,
    ),

    headlineLarge = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 30.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),

    titleLarge = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),

    bodyLarge = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),

    labelLarge = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)
