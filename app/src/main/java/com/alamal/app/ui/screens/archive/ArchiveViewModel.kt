package com.alamal.app.ui.screens.archive

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alamal.app.data.AppRepository
import com.alamal.app.data.RollbackResult
import com.alamal.app.data.db.entity.BreakEntity
import com.alamal.app.data.db.entity.CreditEntity
import com.alamal.app.data.db.entity.PeriodEntity
import com.alamal.app.data.db.entity.RemoteWorkEntity
import com.alamal.app.data.db.entity.WorkDayEntity
import com.alamal.app.domain.MoneyCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PeriodDetails(
    val period: PeriodEntity,
    val workDays: List<WorkDayEntity>,
    val breaksByDay: Map<Long, List<BreakEntity>>,
    val remoteWorks: List<RemoteWorkEntity>,
    val credits: List<CreditEntity>,
    val totalMinutes: Long,
)

data class ArchiveUiState(
    val loading: Boolean = true,
    val periods: List<PeriodEntity> = emptyList(),
    val expandedPeriodId: Long? = null,
    val details: PeriodDetails? = null,
    val loadingDetails: Boolean = false,
)

class ArchiveViewModel(private val repo: AppRepository) : ViewModel() {

    private val expandedId = MutableStateFlow<Long?>(null)
    private val detailsState = MutableStateFlow<PeriodDetails?>(null)
    private val loadingDetails = MutableStateFlow(false)

    val state: StateFlow<ArchiveUiState> = combine(
        repo.allPeriods,
        expandedId,
        detailsState,
        loadingDetails,
    ) { periods, exp, details, loadingDet ->
        ArchiveUiState(
            loading = false,
            periods = periods,
            expandedPeriodId = exp,
            details = details,
            loadingDetails = loadingDet,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ArchiveUiState(),
    )

    fun toggleExpand(periodId: Long) {
        if (expandedId.value == periodId) {
            expandedId.value = null
            detailsState.value = null
            return
        }
        expandedId.value = periodId
        detailsState.value = null
        loadingDetails.value = true
        viewModelScope.launch {
            val period = repo.getPeriod(periodId)
            if (period == null) {
                loadingDetails.value = false
                return@launch
            }
            val workDays = repo.getWorkDaysForPeriod(periodId)
            val breaksMap = repo.getBreaksForDays(workDays.map { it.id })
            val remotes = repo.getRemoteWorksForPeriod(periodId)
            val credits = repo.getCreditsForPeriod(periodId)
            var minutes = 0L
            workDays.forEach { day ->
                minutes += MoneyCalculator.computeDayStats(
                    day = day,
                    breaks = breaksMap[day.id].orEmpty(),
                ).actualMinutes
            }
            detailsState.value = PeriodDetails(
                period = period,
                workDays = workDays,
                breaksByDay = breaksMap,
                remoteWorks = remotes,
                credits = credits,
                totalMinutes = minutes,
            )
            loadingDetails.value = false
        }
    }

    fun rollbackLastPayment(onResult: (RollbackResult) -> Unit) {
        viewModelScope.launch {
            val result = repo.rollbackLastPayment()
            expandedId.value = null
            detailsState.value = null
            onResult(result)
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ArchiveViewModel(AppRepository.get(context.applicationContext)) as T
                }
            }
    }
}
