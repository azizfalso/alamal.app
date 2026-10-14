package com.alamal.app.domain

import com.alamal.app.data.db.entity.BreakEntity
import com.alamal.app.data.db.entity.WorkDayEntity

data class DayStats(
    val actualMinutes: Long,
    val breakMinutes: Long,
    val wage: Double,
)

object MoneyCalculator {

    fun computeDayStats(
        day: WorkDayEntity,
        breaks: List<BreakEntity>,
        now: Long = System.currentTimeMillis(),
    ): DayStats {
        val out = day.clockOut ?: now
        val totalMs = (out - day.clockIn).coerceAtLeast(0L)

        val breakMs = breaks.sumOf { br ->
            val span = (br.endTime - br.startTime)
            if (span > 0) span else 0L
        }.coerceAtMost(totalMs)

        val actualMs = (totalMs - breakMs).coerceAtLeast(0L)

        val actualMinutes = actualMs / 60_000L
        val breakMinutes = breakMs / 60_000L
        val wage = (actualMinutes.toDouble() / 60.0) * day.hourlyRate

        return DayStats(
            actualMinutes = actualMinutes,
            breakMinutes = breakMinutes,
            wage = wage,
        )
    }

    fun computeNet(
        carriedBalance: Double,
        totalWages: Double,
        totalRemote: Double,
        totalCredits: Double,
    ): Double = carriedBalance + totalWages + totalRemote - totalCredits
}
