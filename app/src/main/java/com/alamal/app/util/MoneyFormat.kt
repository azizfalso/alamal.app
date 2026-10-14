package com.alamal.app.util

import java.util.Locale
import kotlin.math.abs

/**
 * Money formatting with Latin digits, per the app's requirement.
 * Examples:
 *   10.0      → "10 DH"
 *   10.35     → "10.35 DH"
 *   1234.5    → "1234.50 DH"
 *   -25.0     → "-25 DH"
 */
object MoneyFormat {

    fun format(amount: Double, currency: String = "DH"): String {
        val rounded = Math.round(amount * 100.0) / 100.0
        val text = if (rounded == rounded.toLong().toDouble()) {
            String.format(Locale.US, "%.0f", rounded)
        } else {
            String.format(Locale.US, "%.2f", rounded)
        }
        return "$text $currency"
    }

    /** Same as [format] but with a leading minus if the amount is negative. */
    fun formatSigned(amount: Double, currency: String = "DH"): String {
        val sign = if (amount < -0.0001) "-" else ""
        return sign + format(abs(amount), currency)
    }

    /** Just the number, no currency. Useful in compact UI badges. */
    fun number(amount: Double): String {
        val rounded = Math.round(amount * 100.0) / 100.0
        return if (rounded == rounded.toLong().toDouble()) {
            String.format(Locale.US, "%.0f", rounded)
        } else {
            String.format(Locale.US, "%.2f", rounded)
        }
    }
}
