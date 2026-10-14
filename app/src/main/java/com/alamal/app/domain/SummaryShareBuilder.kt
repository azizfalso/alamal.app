package com.alamal.app.domain

import com.alamal.app.data.PeriodSnapshot
import com.alamal.app.data.db.entity.BreakEntity
import com.alamal.app.data.db.entity.CreditEntity
import com.alamal.app.data.db.entity.RemoteWorkEntity
import com.alamal.app.util.ArabicPlural
import com.alamal.app.util.DateFormatters
import com.alamal.app.util.MoneyFormat

data class SummaryDay(
    val clockIn: Long,
    val clockOut: Long?,
    val breaks: List<BreakEntity>,
    val actualMinutes: Long,
    val wage: Double,
    val note: String?,
)

data class SummaryInput(
    val startDate: Long,
    val endDate: Long,
    val carriedBalance: Double,
    val totalWages: Double,
    val totalRemote: Double,
    val totalCredits: Double,
    val amountDue: Double,
    val totalMinutes: Long,
    val days: List<SummaryDay>,
    val remotes: List<RemoteWorkEntity>,
    val credits: List<CreditEntity>,
)

object SummaryShareBuilder {

    fun fromSnapshot(snap: PeriodSnapshot): SummaryInput {
        val days = snap.workDays.map { day ->
            val brk = snap.breaksByDay[day.id].orEmpty()
            val stats = MoneyCalculator.computeDayStats(day, brk)
            SummaryDay(
                clockIn = day.clockIn,
                clockOut = day.clockOut,
                breaks = brk,
                actualMinutes = stats.actualMinutes,
                wage = stats.wage,
                note = day.note,
            )
        }
        return SummaryInput(
            startDate = snap.startDate,
            endDate = snap.endDate,
            carriedBalance = snap.carriedBalance,
            totalWages = snap.totalWages,
            totalRemote = snap.totalRemote,
            totalCredits = snap.totalCredits,
            amountDue = snap.amountDue,
            totalMinutes = snap.totalMinutes(),
            days = days,
            remotes = snap.remoteWorks,
            credits = snap.credits,
        )
    }

    fun build(
        input: SummaryInput,
        currency: String,
        includeDetails: Boolean,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("ملخص الدفعة")
        sb.appendLine(
            "الفترة: ${DateFormatters.dateArabicShort(input.startDate)} ← ${DateFormatters.dateArabicShort(input.endDate)}"
        )
        sb.appendLine()
        sb.appendLine("الأيام: ${ArabicPlural.days(input.days.count { it.clockOut != null }.toLong())}")
        sb.appendLine("الساعات: ${ArabicPlural.hoursMinutes(input.totalMinutes)}")
        sb.appendLine("الأجور: ${MoneyFormat.format(input.totalWages, currency)}")
        if (input.totalRemote > 0.0001) {
            sb.appendLine("منها عن بُعد: ${MoneyFormat.format(input.totalRemote, currency)}")
        }
        if (input.totalCredits > 0.0001) {
            sb.appendLine("الكريدي: - ${MoneyFormat.format(input.totalCredits, currency)}")
        }
        if (kotlin.math.abs(input.carriedBalance) > 0.0001) {
            sb.appendLine("الرصيد المرحَّل: ${MoneyFormat.formatSigned(input.carriedBalance, currency)}")
        }
        sb.appendLine("الصافي: ${MoneyFormat.formatSigned(input.amountDue, currency)}")

        if (includeDetails) {
            sb.appendLine()
            sb.appendLine("— التفاصيل —")
            sb.appendLine()
            input.days.forEachIndexed { idx, day ->
                sb.appendLine("${idx + 1}. ${DateFormatters.dateArabic(day.clockIn)}")
                if (day.clockOut == null) {
                    sb.appendLine("   لم يُسجَّل خروج بعد")
                } else {
                    val breaksText = if (day.breaks.isEmpty()) "بدون استراحة"
                    else "${day.breaks.size} استراحة"
                    sb.appendLine(
                        "   ${DateFormatters.time(day.clockIn)} → ${DateFormatters.time(day.clockOut)} • " +
                            "${ArabicPlural.hoursMinutes(day.actualMinutes)} • $breaksText"
                    )
                }
                sb.appendLine("   الأجرة: ${MoneyFormat.format(day.wage, currency)}")
                if (!day.note.isNullOrBlank()) {
                    sb.appendLine("   ملاحظة: ${day.note}")
                }
                sb.appendLine()
            }
            if (input.remotes.isNotEmpty()) {
                sb.appendLine("— العمل عن بُعد —")
                input.remotes.forEach { r ->
                    sb.appendLine("${DateFormatters.dateArabicShort(r.date)} • ${MoneyFormat.format(r.amount, currency)}")
                    if (!r.note.isNullOrBlank()) sb.appendLine("   ${r.note}")
                }
                sb.appendLine()
            }
            if (input.credits.isNotEmpty()) {
                sb.appendLine("— الكريدي —")
                input.credits.forEach { c ->
                    sb.appendLine("${DateFormatters.dateArabicShort(c.date)} • - ${MoneyFormat.format(c.amount, currency)}")
                    if (!c.note.isNullOrBlank()) sb.appendLine("   ${c.note}")
                }
                sb.appendLine()
            }
        }
        return sb.toString().trimEnd()
    }
}
