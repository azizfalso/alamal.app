package com.alamal.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---------- Warm Claude-inspired palette (gold/olive) ----------
// Rule: every neutral has a warm (yellow-brown) undertone.
// The primary accent is now a gold-olive tone instead of terracotta.

// Light surfaces
val Parchment = Color(0xFFF5F4ED)      // app background (light beige)
val Ivory = Color(0xFFFAF9F5)          // card surface (cream)
val WarmSand = Color(0xFFE8E6DC)       // secondary surface
val PureWhite = Color(0xFFFFFFFF)

// Text (light theme)
val NearBlack = Color(0xFF141413)      // primary text
val OliveGray = Color(0xFF5E5D59)      // secondary text
val StoneGray = Color(0xFF87867F)      // tertiary text
val CharcoalWarm = Color(0xFF4D4C48)   // on warm sand

// Borders (light)
val BorderCream = Color(0xFFF0EEE6)    // soft, barely visible
val BorderWarm = Color(0xFFE8E6DC)     // prominent
val RingWarm = Color(0xFFD1CFC5)       // interactive ring
val RingDeep = Color(0xFFC2C0B6)       // pressed

// Brand / accent — gold-olive
val GoldOlive = Color(0xFF8B7548)      // primary CTA
val GoldOliveHover = Color(0xFF75623C) // accent pressed
val GoldOliveLight = Color(0xFFA89368) // lighter variant for dark theme

// Semantic
val SuccessGreen = Color(0xFF17A34A)
val WarnGold = Color(0xFFEAB308)
val ErrorCrimson = Color(0xFFB53333)
val FocusBlue = Color(0xFF3898EC)      // cool-only accent for focus outlines

// Dark theme surfaces
val DarkDeep = Color(0xFF141413)       // page background (dark)
val DarkSurface = Color(0xFF30302E)    // cards (dark)
val DarkElevated = Color(0xFF3D3D3A)   // slightly lighter
val WarmSilver = Color(0xFFB0AEA5)     // text on dark
val DarkBorder = Color(0xFF30302E)

// Legacy aliases kept for backwards compatibility with existing code.
val Terracotta = GoldOlive
val TerracottaHover = GoldOliveHover
val Coral = GoldOliveLight
