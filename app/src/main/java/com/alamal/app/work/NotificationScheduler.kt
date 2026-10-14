package com.alamal.app.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules / cancels the periodic long-shift check.
 * Android limits periodic work to a minimum of 15 minutes.
 */
object NotificationScheduler {

    private const val WORK_NAME = "alamal_long_shift_check"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<LongShiftWorker>(
            30, TimeUnit.MINUTES,
        )
            .setInitialDelay(30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
