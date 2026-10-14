package com.alamal.app.data

import android.content.Context
import com.alamal.app.data.db.AppDatabase
import com.alamal.app.data.db.entity.BreakEntity
import com.alamal.app.data.db.entity.CreditEntity
import com.alamal.app.data.db.entity.PeriodEntity
import com.alamal.app.data.db.entity.RemoteWorkEntity
import com.alamal.app.data.db.entity.WorkDayEntity
import com.alamal.app.domain.MoneyCalculator
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map

data class PeriodSnapshot(
    val startDate: Long,
    val endDate: Long,
    val workDays: List<WorkDayEntity>,
    val breaksByDay: Map<Long, List<BreakEntity>>,
    val remoteWorks: List<RemoteWorkEntity>,
    val credits: List<CreditEntity>,
    val carriedBalance: Double,
    val totalWages: Double,
    val totalRemote: Double,
    val totalCredits: Double,
    val amountDue: Double,
) {
    fun totalMinutes(): Long {
        var mins = 0L
        workDays.forEach { day ->
            val stats = MoneyCalculator.computeDayStats(day, breaksByDay[day.id].orEmpty())
            mins += stats.actualMinutes
        }
        return mins
    }
}

sealed class RollbackResult {
    data class Success(val restoredRecords: Int) : RollbackResult()
    object NoPayments : RollbackResult()
    data class Blocked(val newRecordsCount: Int) : RollbackResult()
}

class AppRepository private constructor(context: Context) {

    private val db = AppDatabase.get(context)

    private val _dataChanged = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val dataChanged: SharedFlow<Unit> = _dataChanged.asSharedFlow()

    private fun notifyChanged() {
        _dataChanged.tryEmit(Unit)
    }

    // ---- Current period flows ----
    val currentWorkDays: Flow<List<WorkDayEntity>> = db.workDayDao().observeCurrent()
    val breaksForCurrentPeriod: Flow<List<BreakEntity>> = db.breakDao().observeForCurrentPeriod()
    val currentRemoteWorks: Flow<List<RemoteWorkEntity>> = db.remoteWorkDao().observeCurrent()
    val currentCredits: Flow<List<CreditEntity>> = db.creditDao().observeCurrent()
    val allPeriods: Flow<List<PeriodEntity>> = db.periodDao().observeAll()

    // ---- All records (for stats) ----
    val allWorkDays: Flow<List<WorkDayEntity>> = db.workDayDao().observeAll()
    val allBreaks: Flow<List<BreakEntity>> = db.breakDao().observeAll()
    val allRemoteWorks: Flow<List<RemoteWorkEntity>> = db.remoteWorkDao().observeAll()
    val allCredits: Flow<List<CreditEntity>> = db.creditDao().observeAll()

    val carriedBalance: Flow<Double> = db.periodDao().observeAll().map { periods ->
        periods.firstOrNull()?.remaining ?: 0.0
    }

    // ---- Work days ----
    suspend fun getOpenWorkDay(): WorkDayEntity? = db.workDayDao().getOpen()
    suspend fun getWorkDay(id: Long): WorkDayEntity? = db.workDayDao().getById(id)
    suspend fun getBreaksForDay(dayId: Long): List<BreakEntity> = db.breakDao().getForDay(dayId)

    suspend fun clockIn(hourlyRate: Double, at: Long = System.currentTimeMillis()): Long {
        val id = db.workDayDao().insert(
            WorkDayEntity(
                periodId = null,
                clockIn = at,
                clockOut = null,
                hourlyRate = hourlyRate,
            )
        )
        notifyChanged()
        return id
    }

    suspend fun upsertWorkDay(day: WorkDayEntity, breaks: List<BreakEntity>): Long {
        val dayId: Long = if (day.id == 0L) {
            db.workDayDao().insert(day)
        } else {
            db.workDayDao().update(day)
            day.id
        }
        db.breakDao().deleteForDay(dayId)
        breaks.forEach { b ->
            db.breakDao().insert(b.copy(id = 0L, workDayId = dayId))
        }
        notifyChanged()
        return dayId
    }

    suspend fun clockOut(dayId: Long, outTime: Long, breaks: List<BreakEntity>) {
        val day = db.workDayDao().getById(dayId) ?: return
        db.workDayDao().update(day.copy(clockOut = outTime))
        db.breakDao().deleteForDay(dayId)
        breaks.forEach { b ->
            db.breakDao().insert(b.copy(id = 0L, workDayId = dayId))
        }
        notifyChanged()
    }

    suspend fun deleteWorkDay(dayId: Long) {
        db.breakDao().deleteForDay(dayId)
        val day = db.workDayDao().getById(dayId) ?: return
        db.workDayDao().delete(day)
        notifyChanged()
    }

    // ---- Remote work ----
    suspend fun getRemoteWork(id: Long): RemoteWorkEntity? = db.remoteWorkDao().getById(id)

    suspend fun saveRemoteWork(item: RemoteWorkEntity) {
        if (item.id == 0L) db.remoteWorkDao().insert(item)
        else db.remoteWorkDao().update(item)
        notifyChanged()
    }

    suspend fun deleteRemoteWork(item: RemoteWorkEntity) {
        db.remoteWorkDao().delete(item)
        notifyChanged()
    }

    // ---- Credits ----
    suspend fun getCredit(id: Long): CreditEntity? = db.creditDao().getById(id)

    suspend fun saveCredit(item: CreditEntity) {
        if (item.id == 0L) db.creditDao().insert(item)
        else db.creditDao().update(item)
        notifyChanged()
    }

    suspend fun deleteCredit(item: CreditEntity) {
        db.creditDao().delete(item)
        notifyChanged()
    }

    // ---- Payment ----
    suspend fun computeCurrentSnapshot(now: Long = System.currentTimeMillis()): PeriodSnapshot {
        val workDays = db.workDayDao().getCurrent()
        val remoteWorks = db.remoteWorkDao().getCurrent()
        val credits = db.creditDao().getCurrent()

        val breaksByDay: Map<Long, List<BreakEntity>> = workDays.associate { day ->
            day.id to db.breakDao().getForDay(day.id)
        }

        val lastPeriod = db.periodDao().getLast()
        val carried = lastPeriod?.remaining ?: 0.0

        var totalWages = 0.0
        workDays.forEach { day ->
            val stats = MoneyCalculator.computeDayStats(day, breaksByDay[day.id].orEmpty())
            totalWages += stats.wage
        }

        val totalRemote = remoteWorks.sumOf { it.amount }
        val totalCredits = credits.sumOf { it.amount }
        val amountDue = MoneyCalculator.computeNet(
            carriedBalance = carried,
            totalWages = totalWages,
            totalRemote = totalRemote,
            totalCredits = totalCredits,
        )

        val startDate: Long = lastPeriod?.endDate
            ?: (workDays.minOfOrNull { it.clockIn }
                ?: remoteWorks.minOfOrNull { it.date }
                ?: credits.minOfOrNull { it.date }
                ?: now)

        return PeriodSnapshot(
            startDate = startDate,
            endDate = now,
            workDays = workDays,
            breaksByDay = breaksByDay,
            remoteWorks = remoteWorks,
            credits = credits,
            carriedBalance = carried,
            totalWages = totalWages,
            totalRemote = totalRemote,
            totalCredits = totalCredits,
            amountDue = amountDue,
        )
    }

    suspend fun receivePayment(
        amountReceived: Double,
        note: String?,
        now: Long = System.currentTimeMillis(),
    ): Long {
        val snapshot = computeCurrentSnapshot(now)
        val remaining = snapshot.amountDue - amountReceived

        val period = PeriodEntity(
            startDate = snapshot.startDate,
            endDate = snapshot.endDate,
            carriedBalance = snapshot.carriedBalance,
            totalWages = snapshot.totalWages,
            totalRemote = snapshot.totalRemote,
            totalCredits = snapshot.totalCredits,
            amountDue = snapshot.amountDue,
            amountReceived = amountReceived,
            remaining = remaining,
            note = note,
            archivedAt = now,
        )
        val periodId = db.periodDao().insert(period)

        db.workDayDao().assignCurrentToPeriod(periodId)
        db.remoteWorkDao().assignCurrentToPeriod(periodId)
        db.creditDao().assignCurrentToPeriod(periodId)

        notifyChanged()
        return periodId
    }

    suspend fun rollbackLastPayment(): RollbackResult {
        val last = db.periodDao().getLast() ?: return RollbackResult.NoPayments

        val newWorkDays = db.workDayDao().getCurrent()
        val newRemotes = db.remoteWorkDao().getCurrent()
        val newCredits = db.creditDao().getCurrent()
        val newCount = newWorkDays.size + newRemotes.size + newCredits.size

        if (newCount > 0) {
            return RollbackResult.Blocked(newCount)
        }

        db.workDayDao().releaseFromPeriod(last.id)
        db.remoteWorkDao().releaseFromPeriod(last.id)
        db.creditDao().releaseFromPeriod(last.id)
        db.periodDao().delete(last)

        val restored = db.workDayDao().getCurrent().size +
            db.remoteWorkDao().getCurrent().size +
            db.creditDao().getCurrent().size

        notifyChanged()
        return RollbackResult.Success(restored)
    }

    // ---- Archive helpers ----
    suspend fun getPeriod(id: Long): PeriodEntity? = db.periodDao().getById(id)

    suspend fun getWorkDaysForPeriod(periodId: Long): List<WorkDayEntity> =
        db.workDayDao().getByPeriod(periodId)

    suspend fun getBreaksForDays(dayIds: List<Long>): Map<Long, List<BreakEntity>> {
        val out = mutableMapOf<Long, List<BreakEntity>>()
        dayIds.forEach { id ->
            out[id] = db.breakDao().getForDay(id)
        }
        return out
    }

    suspend fun getRemoteWorksForPeriod(periodId: Long): List<RemoteWorkEntity> =
        db.remoteWorkDao().getByPeriod(periodId)

    suspend fun getCreditsForPeriod(periodId: Long): List<CreditEntity> =
        db.creditDao().getByPeriod(periodId)

    companion object {
        @Volatile
        private var instance: AppRepository? = null

        fun get(context: Context): AppRepository =
            instance ?: synchronized(this) {
                instance ?: AppRepository(context.applicationContext).also { instance = it }
            }
    }
}
