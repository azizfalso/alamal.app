package com.alamal.app.ui.screens.home

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
import com.alamal.app.domain.SummaryShareBuilder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val carriedBalance: Double = 0.0,
    val totalWages: Double = 0.0,
    val totalRemote: Double = 0.0,
    val totalCredits: Double = 0.0,
    val netBalance: Double = 0.0,
    val totalMinutes: Long = 0L,
    val workDaysCount: Int = 0,
    val openWorkDay: WorkDayEntity? = null,
    val currency: String = "DH",
    val hourlyRate: Double = 10.0,
)

private data class RawData(
    val workDays: List<WorkDayEntity>,
    val breaks: List<BreakEntity>,
    val remotes: List<RemoteWorkEntity>,
    val credits: List<CreditEntity>,
)

class HomeViewModel(
    private val repo: AppRepository,
    settingsRepo: SettingsRepository,
) : ViewModel() {

    private val rawData = combine(
        repo.currentWorkDays,
        repo.breaksForCurrentPeriod,
        repo.currentRemoteWorks,
        repo.currentCredits,
    ) { workDays, breaks, remotes, credits ->
        RawData(workDays, breaks, remotes, credits)
    }

    val state = combine(
        rawData,
        repo.carriedBalance,
        settingsRepo.settings,
    ) { raw, carried, settings: AppSettings ->
        val now = System.currentTimeMillis()
        val breaksByDay = raw.breaks.groupBy { it.workDayId }

        var totalWages = 0.0
        var totalMinutes = 0L
        raw.workDays.forEach { day ->
            val stats = MoneyCalculator.computeDayStats(
                day = day,
                breaks = breaksByDay[day.id].orEmpty(),
                now = now,
            )
            totalWages += stats.wage
            totalMinutes += stats.actualMinutes
        }

        val totalRemote = raw.remotes.sumOf { it.amount }
        val totalCredits = raw.credits.sumOf { it.amount }
        val net = MoneyCalculator.computeNet(
            carriedBalance = carried,
            totalWages = totalWages,
            totalRemote = totalRemote,
            totalCredits = totalCredits,
        )

        HomeUiState(
            loading = false,
            carriedBalance = carried,
            totalWages = totalWages,
            totalRemote = totalRemote,
            totalCredits = totalCredits,
            netBalance = net,
            totalMinutes = totalMinutes,
            workDaysCount = raw.workDays.count { it.clockOut != null },
            openWorkDay = raw.workDays.firstOrNull { it.clockOut == null },
            currency = settings.currency,
            hourlyRate = settings.hourlyRate,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun clockIn() {
        viewModelScope.launch {
            val rate = state.value.hourlyRate
            repo.clockIn(hourlyRate = rate)
        }
    }

    /** Quick clock-out: stop the running work session immediately. */
    fun clockOutNow() {
        viewModelScope.launch {
            val open = repo.getOpenWorkDay() ?: return@launch
            repo.setClockOut(open.id, System.currentTimeMillis())
        }
    }

    /**
     * Cancel the currently open clock-in entirely.
     * Removes the work day (with any breaks) and shows an undo snackbar.
     */
    fun cancelClockIn() {
        viewModelScope.launch {
            val open = repo.getOpenWorkDay() ?: return@launch
            repo.deleteWorkDayWithUndo(open.id)
        }
    }

    fun deleteWorkDay(id: Long) {
        viewModelScope.launch { repo.deleteWorkDay(id) }
    }

    fun deleteRemoteWork(item: RemoteWorkEntity) {
        viewModelScope.launch { repo.deleteRemoteWork(item) }
    }

    fun deleteCredit(item: CreditEntity) {
        viewModelScope.launch { repo.deleteCredit(item) }
    }

    suspend fun buildCurrentSummaryText(includeDetails: Boolean): String {
        val snapshot = repo.computeCurrentSnapshot()
        val input = SummaryShareBuilder.fromSnapshot(snapshot)
        return SummaryShareBuilder.build(
            input = input,
            currency = state.value.currency,
            includeDetails = includeDetails,
        )
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = context.applicationContext
                    return HomeViewModel(
                        repo = AppRepository.get(app),
                        settingsRepo = SettingsRepository(app),
                    ) as T
                }
            }
    }
}
