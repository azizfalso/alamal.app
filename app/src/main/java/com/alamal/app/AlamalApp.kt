package com.alamal.app

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import com.alamal.app.data.AppRepository
import com.alamal.app.data.backup.BackupCoordinator
import com.alamal.app.data.prefs.AppSettings
import com.alamal.app.data.prefs.SettingsRepository
import com.alamal.app.lock.AppLockController
import com.alamal.app.lock.AppLockService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class AlamalApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Job that will lock the app after the background timeout. */
    @Volatile
    private var pendingLockJob: Job? = null

    /** Count of currently started activities (Activities + 1). */
    @Volatile
    private var startedActivities = 0

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()

        val repo = AppRepository.get(this)
        val settingsRepo = SettingsRepository(this)
        val coordinator = BackupCoordinator(this)

        // ---- Auto backup pipeline ----
        appScope.launch {
            repo.dataChanged
                .debounce(AUTO_BACKUP_DEBOUNCE_MS)
                .collect {
                    runCatching { coordinator.performAutoBackupIfEnabled() }
                }
        }

        // ---- Lock configuration ----
        appScope.launch {
            settingsRepo.settings.collect { s: AppSettings ->
                AppLockController.configure(s.pin)
            }
        }

        // ---- App-level foreground/background observer ----
        // Use ActivityLifecycleCallbacks — no extra dependency needed.
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                startedActivities++
                // App is back in foreground → cancel pending lock.
                pendingLockJob?.cancel()
                pendingLockJob = null
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities--
                if (startedActivities <= 0) {
                    scheduleLockOnBackground()
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        // ---- Best-effort: catch swipe-away from Recents + screen off ----
        try {
            startService(Intent(this, AppLockService::class.java))
        } catch (_: Throwable) {
            // Non-fatal — lock still works via ActivityLifecycleCallbacks.
        }
    }

    private fun scheduleLockOnBackground() {
        if (!AppLockController.isEnabled()) return
        pendingLockJob?.cancel()
        pendingLockJob = appScope.launch {
            delay(BACKGROUND_LOCK_DELAY_MS)
            AppLockController.lock()
        }
    }

    private companion object {
        const val AUTO_BACKUP_DEBOUNCE_MS = 3_000L
        const val BACKGROUND_LOCK_DELAY_MS = 30_000L
    }
}
