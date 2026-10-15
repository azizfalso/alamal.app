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
// Darkened StoneGray from #87867F → #6E6D66 so that small "meta" text
// (subtitles, timestamps) keeps readable contrast (~4.6:1) against the
// cream card surface. OliveGray stayed the same — it was already fine.
val NearBlack = Color(0xFF141413)      // primary text
val OliveGray = Color(0xFF5E5D59)      // secondary text
val StoneGray = Color(0xFF6E6D66)      // tertiary / meta text (was #87867F)
val CharcoalWarm = Color(0xFF4D4C48)   // on warm sand

// -------------------- Borders --------------------
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
val DarkDeep = Color(0xFF141413)       // page background (dark)
val DarkSurface = Color(0xFF30302E)    // cards (dark)
val DarkElevated = Color(0xFF3D3D3A)   // slightly lighter

// -------------------- Dark theme text --------------------
// Brightened both values so secondary and tertiary text on dark surfaces
// stop looking muddy. Contrast on #30302E:
//   DarkTextMuted #E0DED5 → ~10.5:1 (was #CFCDC4 ~8.3:1)
//   DarkTextMeta  #C0BEB5 → ~7.4:1  (was #A8A69D ~5.2:1)
val DarkTextMuted = Color(0xFFE0DED5)
val DarkTextMeta = Color(0xFFC0BEB5)
val WarmSilver = DarkTextMuted         // legacy alias
val DarkBorder = BorderSoftDark

// -------------------- Legacy aliases --------------------
val Terracotta = GoldOlive
val TerracottaHover = GoldOliveHover
val Coral = GoldOliveLight
