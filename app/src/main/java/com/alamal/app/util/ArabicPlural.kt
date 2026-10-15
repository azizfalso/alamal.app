package com.alamal.app.util

object ArabicPlural {

    /**
     * U+200F RIGHT-TO-LEFT MARK.
     *
     * When Arabic text mixes with Western digits ("10 ساعات"), the Unicode
     * bidi algorithm can reorder the segments on some devices and render
     * the digits and words out of visual order. Wrapping the phrase in
     * RLMs forces it to be treated as a single RTL run, which keeps the
     * digits embedded in the right place.
     */
    private const val RLM = "\u200F"

    fun hours(n: Long): String = when {
        n == 0L -> "0 ساعة"
        n == 1L -> "ساعة واحدة"
        n == 2L -> "ساعتان"
        n in 3L..10L -> "$n ساعات"
        else -> "$n ساعة"
    }

    fun minutes(n: Long): String = when {
        n == 0L -> "0 دقيقة"
        n == 1L -> "دقيقة واحدة"
        n == 2L -> "دقيقتان"
        n in 3L..10L -> "$n دقائق"
        else -> "$n دقيقة"
    }

    /**
     * Compact, bidi-safe duration. Returns e.g. "10 س 10 د".
     * The full-word version was too long and caused line wraps and
     * bidi reordering on some devices.
     */
    fun hoursMinutes(totalMinutes: Long): String {
        val h = totalMinutes / 60L
        val m = totalMinutes % 60L
        val text = when {
            h == 0L && m == 0L -> "0 د"
            h == 0L -> "$m د"
            m == 0L -> "$h س"
            else -> "$h س $m د"
        }
        return "$RLM$text$RLM"
    }

    fun days(n: Long): String = when {
        n == 0L -> "0 يوم"
        n == 1L -> "يوم واحد"
        n == 2L -> "يومان"
        n in 3L..10L -> "$n أيام"
        else -> "$n يوم"
    }
}
