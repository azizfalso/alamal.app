package com.alamal.app.ui.screens.records

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
import com.alamal.app.util.ArabicPlural
import com.alamal.app.util.DateFormatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class RecordType { WORK_DAY, REMOTE, CREDIT }

enum class RecordFilter { ALL, WORK, REMOTE, CREDIT }

data class RecordItem(
    val id: Long,
    val type: RecordType,
    val dateMs: Long,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val note: String?,
)

data class WeekGroup(
    val weekStart: Long,
    val weekEnd: Long,
    val items: List<RecordItem>,
    val weekNet: Double,
)

data class RecordsUiState(
    val loading: Boolean = true,
    val weeks: List<WeekGroup> = emptyList(),
    val currency: String = "DH",
    val searchQuery: String = "",
    val filter: RecordFilter = RecordFilter.ALL,
    val totalMatches: Int = 0,
)

private data class RawBundle(
    val workDays: List<WorkDayEntity>,
    val breaks: List<BreakEntity>,
    val remotes: List<RemoteWorkEntity>,
    val credits: List<CreditEntity>,
)

class RecordsViewModel(
    private val repo: AppRepository,
    settingsRepo: SettingsRepository,
) : ViewModel() {

    private val raw = combine(
        repo.currentWorkDays,
        repo.breaksForCurrentPeriod,
        repo.currentRemoteWorks,
        repo.currentCredits,
    ) { w, b, r, c -> RawBundle(w, b, r, c) }

    private val searchQuery = MutableStateFlow("")
    private val filter = MutableStateFlow(RecordFilter.ALL)

    fun setSearchQuery(q: String) {
        searchQuery.value = q
    }

    fun setFilter(f: RecordFilter) {
        filter.value = f
    }

    val state: StateFlow<RecordsUiState> = combine(
        raw,
        settingsRepo.settings,
        searchQuery,
        filter,
    ) { bundle, settings: AppSettings, query, filterType ->

        val breaksByDay = bundle.breaks.groupBy { it.workDayId }
        val allItems = mutableListOf<RecordItem>()

        bundle.workDays.forEach { day ->
            val brk = breaksByDay[day.id].orEmpty()
            val stats = MoneyCalculator.computeDayStats(day, brk)
            val title = if (day.clockOut == null) "أنت في العمل الآن" else "يوم عمل"
            val timeRange = "${DateFormatters.time(day.clockIn)} → ${
                day.clockOut?.let { DateFormatters.time(it) } ?: "—"
            }"
            val breaksText = if (brk.isEmpty()) "" else " • ${brk.size} استراحة"
            val subtitle = "$timeRange • ${ArabicPlural.hoursMinutes(stats.actualMinutes)}$breaksText"
            allItems += RecordItem(
                id = day.id,
                type = RecordType.WORK_DAY,
                dateMs = day.clockIn,
                title = title,
                subtitle = subtitle,
                amount = stats.wage,
                note = day.note,
            )
        }

        bundle.remotes.forEach { r ->
            allItems += RecordItem(
                id = r.id,
                type = RecordType.REMOTE,
                dateMs = r.date,
                title = "عمل عن بُعد",
                subtitle = DateFormatters.dateArabicShort(r.date),
                amount = r.amount,
                note = r.note,
            )
        }

        bundle.credits.forEach { c ->
            allItems += RecordItem(
                id = c.id,
                type = RecordType.CREDIT,
                dateMs = c.date,
                title = "كريدي",
                subtitle = DateFormatters.dateArabicShort(c.date),
                amount = c.amount,
                note = c.note,
            )
        }

        // ---- Apply search + filter ----
        val q = query.trim().lowercase()
        val filtered = allItems.filter { item ->
            val matchesFilter = when (filterType) {
                RecordFilter.ALL -> true
                RecordFilter.WORK -> item.type == RecordType.WORK_DAY
                RecordFilter.REMOTE -> item.type == RecordType.REMOTE
                RecordFilter.CREDIT -> item.type == RecordType.CREDIT
            }
            if (!matchesFilter) return@filter false
            if (q.isEmpty()) return@filter true
            val inTitle = item.title.lowercase().contains(q)
            val inNote = item.note?.lowercase()?.contains(q) == true
            val inSubtitle = item.subtitle.lowercase().contains(q)
            inTitle || inNote || inSubtitle
        }

        // ---- Group by week (Monday) ----
        val weeksMap = mutableMapOf<Long, MutableList<RecordItem>>()
        filtered.forEach { item ->
            val ws = DateFormatters.weekStartMonday(item.dateMs)
            weeksMap.getOrPut(ws) { mutableListOf() }.add(item)
        }

        val weeks = weeksMap.entries
            .sortedByDescending { it.key }
            .map { (ws, list) ->
                val we = DateFormatters.weekEndSunday(ws)
                val net = list.sumOf { it ->
                    when (it.type) {
                        RecordType.WORK_DAY -> it.amount
                        RecordType.REMOTE -> it.amount
                        RecordType.CREDIT -> -it.amount
                    }
                }
                WeekGroup(
                    weekStart = ws,
                    weekEnd = we,
                    items = list.sortedByDescending { it.dateMs },
                    weekNet = net,
                )
            }

        RecordsUiState(
            loading = false,
            weeks = weeks,
            currency = settings.currency,
            searchQuery = query,
            filter = filterType,
            totalMatches = filtered.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecordsUiState(),
    )

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = context.applicationContext
                    return RecordsViewModel(
                        repo = AppRepository.get(app),
                        settingsRepo = SettingsRepository(app),
                    ) as T
                }
            }
    }
}
