package com.alamal.app.data

import android.content.Context
import com.alamal.app.data.db.AppDatabase
import com.alamal.app.data.db.entity.BreakEntity
import com.alamal.app.data.db.entity.CreditEntity
import com.alamal.app.data.db.entity.RemoteWorkEntity
import com.alamal.app.data.db.entity.WorkDayEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single entry point for data access used by UI layer.
 * Holds the Room database and exposes flows + suspend operations.
 *
 * Singleton because the underlying DB is a singleton too.
 */
class AppRepository private constructor(context: Context) {

    private val db = AppDatabase.get(context)

    // ---- Flows (current period) ----
    val currentWorkDays: Flow<List<WorkDayEntity>> = db.workDayDao().observeCurrent()
    val breaksForCurrentPeriod: Flow<List<BreakEntity>> = db.breakDao().observeForCurrentPeriod()
    val currentRemoteWorks: Flow<List<RemoteWorkEntity>> = db.remoteWorkDao().observeCurrent()
    val currentCredits: Flow<List<CreditEntity>> = db.creditDao().observeCurrent()

    /** Carried balance from the most recently archived period, or 0.0 if none. */
    val carriedBalance: Flow<Double> = db.periodDao().observeAll().map { periods ->
        periods.firstOrNull()?.remaining ?: 0.0
    }

    // ---- Work days ----

    suspend fun getOpenWorkDay(): WorkDayEntity? = db.workDayDao().getOpen()

    suspend fun getWorkDay(id: Long): WorkDayEntity? = db.workDayDao().getById(id)

    suspend fun getBreaksForDay(dayId: Long): List<BreakEntity> = db.breakDao().getForDay(dayId)

    /** Insert a new open day (clock-in). */
    suspend fun clockIn(hourlyRate: Double, at: Long = System.currentTimeMillis()): Long {
        return db.workDayDao().insert(
            WorkDayEntity(
                periodId = null,
                clockIn = at,
                clockOut = null,
                hourlyRate = hourlyRate,
            )
        )
    }

    /**
     * Insert or update a work day together with its breaks.
     * Breaks always replace the old ones (delete-then-insert).
     */
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
        return dayId
    }

    /** Close an open day with an out-time and (possibly empty) breaks. */
    suspend fun clockOut(dayId: Long, outTime: Long, breaks: List<BreakEntity>) {
        val day = db.workDayDao().getById(dayId) ?: return
        db.workDayDao().update(day.copy(clockOut = outTime))
        db.breakDao().deleteForDay(dayId)
        breaks.forEach { b ->
            db.breakDao().insert(b.copy(id = 0L, workDayId = dayId))
        }
    }

    suspend fun deleteWorkDay(dayId: Long) {
        db.breakDao().deleteForDay(dayId)
        val day = db.workDayDao().getById(dayId) ?: return
        db.workDayDao().delete(day)
    }

    // ---- Remote work ----

    suspend fun saveRemoteWork(item: RemoteWorkEntity) {
        if (item.id == 0L) db.remoteWorkDao().insert(item)
        else db.remoteWorkDao().update(item)
    }

    suspend fun deleteRemoteWork(item: RemoteWorkEntity) {
        db.remoteWorkDao().delete(item)
    }

    // ---- Credits ----

    suspend fun saveCredit(item: CreditEntity) {
        if (item.id == 0L) db.creditDao().insert(item)
        else db.creditDao().update(item)
    }

    suspend fun deleteCredit(item: CreditEntity) {
        db.creditDao().delete(item)
    }

    companion object {
        @Volatile
        private var instance: AppRepository? = null

        fun get(context: Context): AppRepository =
            instance ?: synchronized(this) {
                instance ?: AppRepository(context.applicationContext).also { instance = it }
            }
    }
}
