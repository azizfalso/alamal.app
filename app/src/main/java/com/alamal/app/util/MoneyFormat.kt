package com.alamal.app.util

import java.util.Locale
import kotlin.math.abs

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

    fun formatSigned(amount: Double, currency: String = "DH"): String {
        val sign = if (amount < -0.0001) "-" else ""
        return sign + format(abs(amount), currency)
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
