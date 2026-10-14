package com.alamal.app.util

import java.util.Calendar
import java.util.Locale

/**
 * Date/time formatting with Latin digits and Arabic month/day names.
 */
object DateFormatters {

    private val dayNames = arrayOf(
        "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"
    )

    private val monthNames = arrayOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    /** "14:35" */
    fun time(ms: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        return String.format(
            Locale.US,
            "%02d:%02d",
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE),
        )
    }

    /** "2026-10-14 14:35" */
    fun dateTime(ms: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        return String.format(
            Locale.US,
            "%04d-%02d-%02d %02d:%02d",
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1
