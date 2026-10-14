package com.alamal.app.util

import java.util.Calendar
import java.util.Locale

/**
 * Date/time formatting with Latin digits and Arabic day/month names.
 * All output uses Latin numerals (0-9) per the app's requirement.
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
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH),
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE),
        )
    }

    /** "2026-10-14" */
    fun dateIso(ms: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH),
        )
    }

    /** "الأربعاء، 14 أكتوبر 2026" */
    fun dateArabic(ms: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        val day = dayNames[c.get(Calendar.DAY_OF_WEEK) - 1]
        val month = monthNames[c.get(Calendar.MONTH)]
        return "$day، ${c.get(Calendar.DAY_OF_MONTH)} $month ${c.get(Calendar.YEAR)}"
    }

    /** "14 أكتوبر" */
    fun dateArabicShort(ms: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        val month = monthNames[c.get(Calendar.MONTH)]
        return "${c.get(Calendar.DAY_OF_MONTH)} $month"
    }

    /** "الأسبوع: 14 أكتوبر — 20 أكتوبر" */
    fun weekLabel(startMs: Long, endMs: Long): String {
        return "الأسبوع: ${dateArabicShort(startMs)} — ${dateArabicShort(endMs)}"
    }

    /** "ساعتان و 15 دقيقة" */
    fun duration(minutes: Long): String = ArabicPlural.hoursMinutes(minutes)

    /** Monday 00:00:00.000 of the week containing ms. */
    fun weekStartMonday(ms: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Calendar.DAY_OF_WEEK: Sunday=1, Monday=2, ..., Saturday=7
        // We want Monday=0 offset ... Sunday=6 offset.
        val dow = c.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = (dow + 5) % 7
        c.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        return c.timeInMillis
    }

    /** Sunday 23:59:59.999 of the week containing ms. */
    fun weekEndSunday(ms: Long): Long {
        val start = weekStartMonday(ms)
        val c = Calendar.getInstance().apply {
            timeInMillis = start
            add(Calendar.DAY_OF_MONTH, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return c.timeInMillis
    }
}
