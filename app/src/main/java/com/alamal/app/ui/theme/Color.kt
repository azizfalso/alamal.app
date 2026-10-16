package com.alamal.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---------- Warm Claude-inspired palette (gold/olive) ----------
// Rule: every neutral has a warm (yellow-brown) undertone.
// The primary accent is a gold-olive tone instead of terracotta.

// -------------------- Light theme surfaces --------------------
val Parchment = Color(0xFFF5F4ED)      // app background (light beige)
val Ivory = Color(0xFFFAF9F5)          // card surface (cream)
val WarmSand = Color(0xFFE8E6DC)       // secondary surface
val PureWhite = Color(0xFFFFFFFF)

// -------------------- Light theme text --------------------
val NearBlack = Color(0xFF141413)      // primary text
val OliveGray = Color(0xFF5E5D59)      // secondary text
val StoneGray = Color(0xFF6E6D66)      // tertiary / meta text (was #87867F)
val CharcoalWarm = Color(0xFF4D4C48)   // on warm sand

// -------------------- Borders (LIGHT) --------------------
// Strengthened so borders are actually visible on the cream card.
// Previous values (#E8E5D8 / #DCD8C8) were almost identical to the card,
// which is why the cards looked borderless in light mode.
val BorderSoftLight = Color(0xFFDCD8C8)
val BorderMediumLight = Color(0xFFC9C4B0)

// -------------------- Borders (DARK) --------------------
// Brightened so card borders stand out on the darker card surface (#2A2A28).
val BorderSoftDark = Color(0xFF5A5852)
val BorderMediumDark = Color(0xFF6A6862)

// Legacy aliases (kept so older imports don't break)
val BorderCream = BorderSoftLight
val BorderWarm = BorderMediumLight
val RingWarm = BorderMediumLight
val RingDeep = Color(0xFFB8B4A4)

// -------------------- Brand / accent --------------------
val GoldOlive = Color(0xFF8B7548)          // primary CTA (light theme)
val GoldOliveHover = Color(0xFF75623C)     // accent pressed (light)
val GoldOliveLight = Color(0xFFA89368)     // legacy alias
val GoldOliveBright = Color(0xFFC9B080)    // dark-theme accent

// -------------------- Semantic --------------------
val SuccessGreen = Color(0xFF17A34A)
val WarnGold = Color(0xFFEAB308)
val ErrorCrimson = Color(0xFFB53333)
val FocusBlue = Color(0xFF3898EC)

// -------------------- Dark theme surfaces --------------------
// Darkened slightly (was #30302E and #3D3D3A). User request:
// "the grey card color a bit darker, not black."
// Still clearly above the page background (#141413).
val DarkDeep = Color(0xFF141413)       // page background
val DarkSurface = Color(0xFF2A2A28)    // card surface
val DarkElevated = Color(0xFF353533)   // secondary surface

// -------------------- Dark theme text --------------------
// Bright enough to read clearly on #2A2A28 without looking washed out.
//   DarkTextMuted #E0DED5 → ~11.3:1
//   DarkTextMeta  #C0BEB5 → ~7.8:1
val DarkTextMuted = Color(0xFFE0DED5)
val DarkTextMeta = Color(0xFFC0BEB5)
val WarmSilver = DarkTextMuted         // legacy alias
val DarkBorder = BorderSoftDark

// -------------------- Legacy aliases --------------------
val Terracotta = GoldOlive
val TerracottaHover = GoldOliveHover
val Coral = GoldOliveLight
