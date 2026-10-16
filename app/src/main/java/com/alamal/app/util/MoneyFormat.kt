package com.alamal.app.util

import java.util.Locale
import kotlin.math.abs

object MoneyFormat {

    /**
     * U+200E LEFT-TO-RIGHT MARK.
     *
     * Forces the following text to be laid out as one LTR run, so
     * "-15 DH" always renders with the minus on the left of the number
     * and "DH" on the right — no matter what RTL context surrounds it.
     * Without this, Android's bidi algorithm can reorder the pieces and
     * the number appears to "jump" between lines.
     */
    private const val LRM = "\u200E"

    /**
     * U+00A0 NO-BREAK SPACE.
     *
     * The space between the number and the currency is the natural wrap
     * point in the old code, which is why "15 DH" split into two lines in
     * narrow cells. A no-break space keeps them together.
     */
    private const val NBSP = "\u00A0"

    fun format(amount: Double, currency: String = "DH"): String {
        val rounded = Math.round(amount * 100.0) / 100.0
        val text = if (rounded == rounded.toLong().toDouble()) {
            String.format(Locale.US, "%.0f", rounded)
        } else {
            String.format(Locale.US, "%.2f", rounded)
        }
        return "$LRM$text$NBSP$currency"
    }

    fun formatSigned(amount: Double, currency: String = "DH"): String {
        val rounded = Math.round(amount * 100.0) / 100.0
        if (rounded == 0.0) return format(0.0, currency)
        val sign = if (rounded < 0) "-" else ""
        return "$LRM$sign${format(abs(rounded), currency).removePrefix(LRM)}"
    }

    fun number(amount: Double): String {
        val rounded = Math.round(amount * 100.0) / 100.0
        return if (rounded == rounded.toLong().toDouble()) {
            String.format(Locale.US, "%.0f", rounded)
        } else {
            String.format(Locale.US, "%.2f", rounded)
        }
    }
}
