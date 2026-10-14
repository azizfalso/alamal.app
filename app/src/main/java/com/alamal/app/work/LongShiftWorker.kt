package com.alamal.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alamal.app.data.AppRepository
import com.alamal.app.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Periodic worker that checks whether the current open work day has exceeded
 * the configured "long shift" threshold and, if so, posts a notification.
 * It only fires once per work day.
 */
class LongShiftWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val settingsRepo = SettingsRepository(ctx)
        val settings = settingsRepo.settings.first()

        if (!settings.notificationsEnabled) return Result.success()

        val repo = AppRepository.get(ctx)
        val open = repo.getOpenWorkDay() ?: return Result.success()

        val elapsedMs = System.currentTimeMillis() - open.clockIn
        val elapsedHours = elapsedMs / (1000.0 * 60.0 * 60.0)

        if (elapsedHours < settings.longShiftHours) return Result.success()

        val lastNotified = settingsRepo.getLastNotifiedDayId()
        if (lastNotified == open.id) return Result.success()

        NotificationHelper.showLongShiftNotification(ctx, elapsedHours)
        settingsRepo.setLastNotifiedDayId(open.id)
        return Result.success()
    }
}
