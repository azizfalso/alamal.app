package com.alamal.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---------- Claude-inspired warm palette ----------
// Rule: every neutral has a warm (yellow-brown) undertone.
// No cool blue-grays. Only one chromatic accent: Terracotta.

// Light surfaces
val Parchment = Color(0xFFF5F4ED)      // app background
val Ivory = Color(0xFFFAF9F5)          // card surface
val WarmSand = Color(0xFFE8E6DC)       // secondary surface
val PureWhite = Color(0xFFFFFFFF)

// Text (light theme)
val NearBlack = Color(0xFF141413)      // primary text
val OliveGray = Color(0xFF5E5D59)      // secondary
val StoneGray = Color(0xFF87867F)      // tertiary
val CharcoalWarm = Color(0xFF4D4C48)   // on warm sand

// Borders (light)
val BorderCream = Color(0xFFF0EEE6)    // soft, barely visible
val BorderWarm = Color(0xFFE8E6DC)     // prominent
val RingWarm = Color(0xFFD1CFC5)       // interactive ring
val RingDeep = Color(0xFFC2C0B6)       // pressed

// Brand / accent
val Terracotta = Color(0xFFC96442)     // primary CTA
val TerracottaHover = Color(0xFFB15535) // accent pressed
val Coral = Color(0xFFD97757)          // accent on dark surfaces

// Semantic
val SuccessGreen = Color(0xFF17A34A)
val WarnGold = Color(0xFFEAB308)
val ErrorCrimson = Color(0xFFB53333)
val FocusBlue = Color(0xFF3898EC)      // the only cool color, for focus only

// Dark theme surfaces
val DarkDeep = Color(0xFF141413)       // page background (dark)
val DarkSurface = Color(0xFF30302E)    // cards (dark)
val DarkElevated = Color(0xFF3D3D3A)   // slightly lighter
val WarmSilver = Color(0xFFB0AEA5)     // text on dark
val DarkBorder = Color(0xFF30302E)
