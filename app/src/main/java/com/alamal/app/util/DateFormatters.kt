package com.alamal.app.util

import java.util.Calendar
import java.util.Locale

object DateFormatters {

    private val dayNames = arrayOf(
        "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"
    )

    private val monthNames = arrayOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    fun time(ms: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        return String.format(
            Locale.US,
            "%02d:%02d",
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE),
        )
    }

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

    fun dateArabic(ms: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        val day = dayNames[c.get(Calendar.DAY_OF_WEEK) - 1]
        val month = monthNames[c.get(Calendar.MONTH)]
        return "$day، ${c.get(Calendar.DAY_OF_MONTH)} $month ${c.get(Calendar.YEAR)}"
    }

    fun dateArabicShort(ms: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        val month = monthNames[c.get(Calendar.MONTH)]
        return "${c.get(Calendar.DAY_OF_MONTH)} $month"
    }

    fun weekLabel(startMs: Long, endMs: Long): String {
        return "الأسبوع: ${dateArabicShort(startMs)} — ${dateArabicShort(endMs)}"
    }

    fun duration(minutes: Long): String = ArabicPlural.hoursMinutes(minutes)

    fun weekStartMonday(ms: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dow = c.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = (dow + 5) % 7
        c.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        return c.timeInMillis
    }

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
