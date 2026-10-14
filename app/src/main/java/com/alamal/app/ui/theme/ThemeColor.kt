package com.alamal.app.ui.theme

data class ColorFamily(
    val nameAr: String,
    val hexColors: List<String>,
)

/**
 * Curated palette families for the new picker.
 * Each family has 6 colors for a compact 1-row grid.
 */
val ColorFamilies: List<ColorFamily> = listOf(
    ColorFamily(
        nameAr = "باردة",
        hexColors = listOf(
            "#4D6BFE", // أزرق
            "#5A6B85", // رمادي أزرق
            "#0E9AA7", // تيركواز
            "#2E8B72", // أخضر زمردي
            "#3F51B5", // نيلي
            "#6A5ACD", // بنفسجي متوسط
        ),
    ),
    ColorFamily(
        nameAr = "دافئة",
        hexColors = listOf(
            "#C2703D", // برتقالي محروق
            "#B8322E", // أحمر قرمزي
            "#C7707A", // وردي ترابي
            "#8B6B4A", // بني دافئ
            "#D4A017", // ذهبي
            "#7A5AF8", // بنفسجي هادئ
        ),
    ),
    ColorFamily(
        nameAr = "طبيعية",
        hexColors = listOf(
            "#4C7A4C", // أخضر زيتي
            "#2F5D3A", // أخضر داكن
            "#6B8E23", // زيتوني
            "#8F9779", // سيج
            "#5D7B6F", // أخضر مائي
            "#A0522D", // سيينا
        ),
    ),
)
