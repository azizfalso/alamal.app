package com.alamal.app.util

object ArabicPlural {

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

    fun hoursMinutes(totalMinutes: Long): String {
        val h = totalMinutes / 60L
        val m = totalMinutes % 60L
        return when {
            h == 0L && m == 0L -> "0 دقيقة"
            h == 0L -> minutes(m)
            m == 0L -> hours(h)
            else -> "${hours(h)} و${minutes(m)}"
        }
    }

    fun days(n: Long): String = when {
        n == 0L -> "0 يوم"
        n == 1L -> "يوم واحد"
        n == 2L -> "يومان"
        n in 3L..10L -> "$n أيام"
        else -> "$n يوم"
    }
}
