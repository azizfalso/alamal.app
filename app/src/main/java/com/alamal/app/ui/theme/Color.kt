package com.alamal.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---------- Warm Claude-inspired palette (gold/olive) ----------
// Rule: every neutral has a warm (yellow-brown) undertone.
// A softer, less saturated set of accents after user feedback that the
// previous palette felt "harsh / glowing". All hues are muted one step.

// -------------------- Light theme surfaces --------------------
val Parchment = Color(0xFFF5F4ED)      // app background (light beige)
val Ivory = Color(0xFFFAF9F5)          // card surface (cream)
val WarmSand = Color(0xFFE8E6DC)       // secondary surface
val PureWhite = Color(0xFFFFFFFF)

// -------------------- Light theme text --------------------
val NearBlack = Color(0xFF141413)
val OliveGray = Color(0xFF5E5D59)
val StoneGray = Color(0xFF6E6D66)
val CharcoalWarm = Color(0xFF4D4C48)

// -------------------- Borders (LIGHT) --------------------
val BorderSoftLight = Color(0xFFDCD8C8)
val BorderMediumLight = Color(0xFFC9C4B0)

// -------------------- Borders (DARK) --------------------
val BorderSoftDark = Color(0xFF4A4844)
val BorderMediumDark = Color(0xFF5A5852)

// Legacy aliases
val BorderCream = BorderSoftLight
val BorderWarm = BorderMediumLight
val RingWarm = BorderMediumLight
val RingDeep = Color(0xFFB8B4A4)

// -------------------- Brand / accent --------------------
// Muted one step further. The previous palette used #8B7548 (light) and
// #C9B080 (dark), which read as "harsh" on the dark theme. Both are now
// desaturated slightly, keeping the same warm hue family.
val GoldOlive = Color(0xFF7E6B42)          // primary CTA (light)
val GoldOliveHover = Color(0xFF6A5935)     // accent pressed (light)
val GoldOliveLight = Color(0xFF96834F)     // legacy alias
val GoldOliveBright = Color(0xFFA48E62)    // dark-theme accent

// -------------------- Surface accents for special cards --------------------
// The clock-in card and the at-work card no longer use solid accent fills.
// They now use soft warm surfaces + a border, which keeps them distinct
// from regular cards without shouting.
val AccentSurfaceLight = Color(0xFFF0ECDD)     // clock-in background, light
val AccentSurfaceBorderLight = Color(0xFFD8CFB8) // clock-in border, light
val AccentSurfaceDark = Color(0xFF322E26)      // clock-in background, dark
val AccentSurfaceBorderDark = Color(0xFF5A5040) // clock-in border, dark

// Working (in-progress) card — a calm olive-green instead of the previous
// saturated #17A34A.
val WorkingSurfaceLight = Color(0xFFE1EDE5)
val WorkingSurfaceDark = Color(0xFF22352A)
val WorkingBorder = Color(0xFF3D7A52)
val WorkingTextLight = Color(0xFF2D5B3E)   // big number color, light
val WorkingTextDark = Color(0xFFA4C8B4)    // big number color, dark

// -------------------- Semantic --------------------
val SuccessGreen = Color(0xFF3D7A52)       // was #17A34A — olive green
val WarnGold = Color(0xFFC9A227)           // was #EAB308 — muted gold
val ErrorCrimson = Color(0xFF9C3838)       // was #B53333 — terracotta
val FocusBlue = Color(0xFF3898EC)

// -------------------- Dark theme surfaces --------------------
val DarkDeep = Color(0xFF141413)
val DarkSurface = Color(0xFF222220)        // was #2A2A28
val DarkElevated = Color(0xFF2E2E2C)       // was #353533

// -------------------- Dark theme text --------------------
val DarkTextMuted = Color(0xFFE0DED5)
val DarkTextMeta = Color(0xFFC0BEB5)
val WarmSilver = DarkTextMuted
val DarkBorder = BorderSoftDark

// -------------------- Legacy aliases --------------------
val Terracotta = GoldOlive
val TerracottaHover = GoldOliveHover
val Coral = GoldOliveLight
