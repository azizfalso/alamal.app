package com.alamal.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---------- Warm Claude-inspired palette (gold/olive) ----------
// Rule: every neutral has a warm (yellow-brown) undertone.
// The primary accent is a gold-olive tone instead of terracotta.

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

// -------------------- Borders --------------------
// PROBLEM (old values): the card surface and its border were nearly identical
// in both themes, so cards looked borderless and the UI read as flat.
//   Light: #FAF9F5 card vs #F0EEE6 border  → ~1.05:1 luminance ratio
//   Dark:  #30302E card vs #30302E border  → 1:1 (invisible!)
// These new tokens are one clear step apart from the card surface.

// Light borders — soft but visible against Ivory (#FAF9F5)
val BorderSoftLight = Color(0xFFE8E5D8)
val BorderMediumLight = Color(0xFFDCD8C8)

// Dark borders — lighter than DarkSurface (#30302E) so cards get a frame
val BorderSoftDark = Color(0xFF4A4945)
val BorderMediumDark = Color(0xFF5A5955)

// Legacy aliases (kept so older imports don't break)
val BorderCream = BorderSoftLight
val BorderWarm = BorderMediumLight
val RingWarm = BorderMediumLight
val RingDeep = Color(0xFFC2C0B6)

// Brand / accent — gold-olive
val GoldOlive = Color(0xFF8B7548)          // primary CTA (light theme)
val GoldOliveHover = Color(0xFF75623C)     // accent pressed (light)
val GoldOliveLight = Color(0xFFA89368)     // legacy alias
val GoldOliveBright = Color(0xFFC9B080)    // dark-theme accent (better contrast)

// Semantic
val SuccessGreen = Color(0xFF17A34A)
val WarnGold = Color(0xFFEAB308)
val ErrorCrimson = Color(0xFFB53333)
val FocusBlue = Color(0xFF3898EC)

// Dark theme surfaces
val DarkDeep = Color(0xFF141413)       // page background (dark)
val DarkSurface = Color(0xFF30302E)    // cards (dark)
val DarkElevated = Color(0xFF3D3D3A)   // slightly lighter

// Dark theme text
// PROBLEM (old): WarmSilver (#B0AEA5) was warm and merged with the warm dark
// background. These are lighter and slightly cooler for clear reading.
val DarkTextMuted = Color(0xFFCFCDC4)  // secondary text on dark
val DarkTextMeta = Color(0xFFA8A69D)   // tertiary text on dark
val WarmSilver = DarkTextMuted         // legacy alias
val DarkBorder = BorderSoftDark

// Legacy aliases kept for backwards compatibility with existing code.
val Terracotta = GoldOlive
val TerracottaHover = GoldOliveHover
val Coral = GoldOliveLight
