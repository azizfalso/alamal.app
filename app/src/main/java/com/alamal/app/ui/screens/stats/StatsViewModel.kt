package com.alamal.app.ui.screens.stats

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alamal.app.data.AppRepository
import com.alamal.app.data.db.entity.BreakEntity
import com.alamal.app.data.db.entity.CreditEntity
import com.alamal.app.data.db.entity.RemoteWorkEntity
import com.alamal.app.data.db.entity.WorkDayEntity
import com.alamal.app.data.prefs.AppSettings
import com.alamal.app.data.prefs.SettingsRepository
import com.alamal.app.domain.MoneyCalculator
import com.alamal.app.util.DateFormatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class DayBar(
    val day: Int,
    val amount: Double,
    val minutes: Long,
)

data class WeekStats(
    val weekStart: Long,
    val weekEnd: Long,
    val label: String,
    val days: List<DayBar>,
    val totalMinutes: Long,
    val totalAmount: Double,
)

data class StatsUiState(
    val loading: Boolean = true,
    val year: Int = 0,
    val month: Int = 0,
    val monthLabel: String = "",
    val daysCount: Int = 0,
    val totalMinutes: Long = 0L,
    val totalWages: Double = 0.0,
    val totalRemote: Double = 0.0,
    val totalCredits: Double = 0.0,
    val net: Double = 0.0,
    val dailyBars: List<DayBar> = emptyList(),
    val weeklyStats: List<WeekStats> = emptyList(),
    val currency: String = "DH",
    val canGoNext: Boolean = false,
    // ---- Advanced stats ----
    val avgHourlyWage: Double = 0.0,
    val avgDailyWage: Double = 0.0,
    val maxDailyMinutes: Long = 0L,
    val maxDailyMinutesDate: Long = 0L,
    val bestDayWage: Double = 0.0,
    val bestDayDate: Long = 0L,
)

private data class DataBundle(
    val workDays: List<WorkDayEntity>,
    val breaks: List<BreakEntity>,
    val remotes: List<RemoteWorkEntity>,
    val credits: List<CreditEntity>,
)

class StatsViewModel(
    repo: AppRepository,
    settingsRepo: SettingsRepository,
) : ViewModel() {

    private val anchor = MutableStateFlow(monthAnchor(System.currentTimeMillis()))

    private val dataFlow = combine(
        repo.allWorkDays,
        repo.allBreaks,
        repo.allRemoteWorks,
        repo.allCredits,
    ) { w, b, r, c -> DataBundle(w, b, r, c) }

    val state = combine(
        anchor,
        dataFlow,
        settingsRepo.settings,
    ) { anchorMs, data, settings: AppSettings ->
        val cal = Calendar.getInstance().apply { timeInMillis = anchorMs }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)

        val breaksByDay = data.breaks.groupBy { it.workDayId }

        var totalWages = 0.0
        var totalMinutes = 0L
        var daysCount = 0
        val byDayAmount = mutableMapOf<Int, Double>()
        val byDayMinutes = mutableMapOf<Int, Long>()

        data.workDays.forEach { day ->
            if (inMonth(day.clockIn, year, month)) {
                val stats = MoneyCalculator.computeDayStats(day, breaksByDay[day.id].orEmpty())
                totalWages += stats.wage
                totalMinutes += stats.actualMinutes
                if (day.clockOut != null) daysCount++
                val d = dayOfMonth(day.clockIn)
                byDayAmount[d] = (byDayAmount[d] ?: 0.0) + stats.wage
                byDayMinutes[d] = (byDayMinutes[d] ?: 0L) + stats.actualMinutes
            }
        }

        var totalRemote = 0.0
        data.remotes.forEach { r ->
            if (inMonth(r.date, year, month)) {
                totalRemote += r.amount
                val d = dayOfMonth(r.date)
                byDayAmount[d] = (byDayAmount[d] ?: 0.0) + r.amount
            }
        }

        var totalCredits = 0.0
        data.credits.forEach { c ->
            if (inMonth(c.date, year, month)) {
                totalCredits += c.amount
                val d = dayOfMonth(c.date)
                byDayAmount[d] = (byDayAmount[d] ?: 0.0) - c.amount
            }
        }

        val net = totalWages + totalRemote - totalCredits
        val maxDay = daysInMonth(year, month)
        val bars = (1..maxDay).map { d ->
            DayBar(
                day = d,
                amount = byDayAmount[d] ?: 0.0,
                minutes = byDayMinutes[d] ?: 0L,
            )
        }

        val weekly = computeWeeklyStats(year, month, byDayAmount, byDayMinutes)

        // ---- Advanced computations ----
        val totalHours = totalMinutes / 60.0
        val avgHourly = if (totalHours > 0.0) totalWages / totalHours else 0.0
        val avgDaily = if (daysCount > 0) totalWages / daysCount else 0.0

        var maxMinutes = 0L
        var maxMinutesDay = 0
        byDayMinutes.forEach { (d, mins) ->
            if (mins > maxMinutes) {
                maxMinutes = mins
                maxMinutesDay = d
            }
        }

        var bestWage = 0.0
        var bestDay = 0
        byDayAmount.forEach { (d, amount) ->
            if (amount > bestWage) {
                bestWage = amount
                bestDay = d
            }
        }

        val firstOfMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val maxMinutesMs = if (maxMinutesDay > 0) {
            (firstOfMonth.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, maxMinutesDay)
            }.timeInMillis
        } else 0L

        val bestDayMs = if (bestDay > 0) {
            (firstOfMonth.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, bestDay)
            }.timeInMillis
        } else 0L

        val nowCal = Calendar.getInstance()
        val isCurrentMonth = nowCal.get(Calendar.YEAR) == year &&
            nowCal.get(Calendar.MONTH) == month

        StatsUiState(
            loading = false,
            year = year,
            month = month,
            monthLabel = "${monthName(month)} $year",
            daysCount = daysCount,
            totalMinutes = totalMinutes,
            totalWages = totalWages,
            totalRemote = totalRemote,
            totalCredits = totalCredits,
            net = net,
            dailyBars = bars,
            weeklyStats = weekly,
            currency = settings.currency,
            canGoNext = !isCurrentMonth,
            avgHourlyWage = avgHourly,
            avgDailyWage = avgDaily,
            maxDailyMinutes = maxMinutes,
            maxDailyMinutesDate = maxMinutesMs,
            bestDayWage = bestWage,
            bestDayDate = bestDayMs,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState(),
    )

    fun goPrev() {
        anchor.value = shiftMonth(anchor.value, -1)
    }

    fun goNext() {
        val now = Calendar.getInstance()
        val nextMs = shiftMonth(anchor.value, 1)
        val next = Calendar.getInstance().apply { timeInMillis = nextMs }
        val notBeyondNow = next.get(Calendar.YEAR) < now.get(Calendar.YEAR) ||
            (next.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                next.get(Calendar.MONTH) <= now.get(Calendar.MONTH))
        if (notBeyondNow) anchor.value = nextMs
    }

    private fun computeWeeklyStats(
        year: Int,
        month: Int,
        byDayAmount: Map<Int, Double>,
        byDayMinutes: Map<Int, Long>,
    ): List<WeekStats> {
        val result = mutableListOf<WeekStats>()
        val firstOfMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val lastDay = firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

        val weeks = linkedMapOf<Long, MutableList<DayBar>>()
        for (d in 1..lastDay) {
            val c = (firstOfMonth.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, d)
            }
            val ms = c.timeInMillis
            val wkStart = DateFormatters.weekStartMonday(ms)
            val bar = DayBar(
                day = d,
                amount = byDayAmount[d] ?: 0.0,
                minutes = byDayMinutes[d] ?: 0L,
            )
            weeks.getOrPut(wkStart) { mutableListOf() }.add(bar)
        }

        weeks.forEach { (wkStart, days) ->
            val slots = MutableList<DayBar?>(7) { null }
            days.forEach { bar ->
                val c = (firstOfMonth.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, bar.day)
                }
                val dow = c.get(Calendar.DAY_OF_WEEK)
                val idx = (dow + 5) % 7
                slots[idx] = bar
            }
            val filled = slots.map { s ->
                s ?: DayBar(day = 0, amount = 0.0, minutes = 0L)
            }
            val wkEnd = DateFormatters.weekEndSunday(wkStart)
            val totalMinutes = filled.sumOf { it.minutes }
            val totalAmount = filled.sumOf { it.amount }
            result += WeekStats(
                weekStart = wkStart,
                weekEnd = wkEnd,
                label = "${DateFormatters.dateArabicShort(wkStart)} ← ${DateFormatters.dateArabicShort(wkEnd)}",
                days = filled,
                totalMinutes = totalMinutes,
                totalAmount = totalAmount,
            )
        }
        return result.sortedBy { it.weekStart }
    }

    private fun monthAnchor(ms: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = ms
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    private fun shiftMonth(ms: Long, delta: Int): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = ms
            add(Calendar.MONTH, delta)
        }
        return c.timeInMillis
    }

    private fun inMonth(ms: Long, year: Int, month: Int): Boolean {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        return c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month
    }

    private fun dayOfMonth(ms: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        return c.get(Calendar.DAY_OF_MONTH)
    }

    private fun daysInMonth(year: Int, month: Int): Int {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return c.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun monthName(month: Int): String = when (month) {
        0 -> "يناير"
        1 -> "فبراير"
        2 -> "مارس"
        3 -> "أبريل"
        4 -> "مايو"
        5 -> "يونيو"
        6 -> "يوليو"
        7 -> "أغسطس"
        8 -> "سبتمبر"
        9 -> "أكتوبر"
        10 -> "نوفمبر"
        else -> "ديسمبر"
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = context.applicationContext
                    return StatsViewModel(
                        repo = AppRepository.get(app),
                        settingsRepo = SettingsRepository(app),
                    ) as T
                }
            }
    }
}
