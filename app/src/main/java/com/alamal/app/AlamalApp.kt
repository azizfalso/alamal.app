package com.alamal.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import com.alamal.app.data.AppRepository
import com.alamal.app.data.backup.BackupCoordinator
import com.alamal.app.data.prefs.AppSettings
import com.alamal.app.data.prefs.SettingsRepository
import com.alamal.app.lock.AppLockController
import com.alamal.app.lock.ScreenLockReceiver
import com.alamal.app.work.NotificationHelper
import com.alamal.app.work.NotificationScheduler
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

    @Volatile
    private var pendingLockJob: Job? = null

    @Volatile
    private var startedActivities = 0

    @Volatile
    private var backgroundedAtMs = 0L

    // Registered dynamically because ACTION_SCREEN_OFF cannot be declared
    // in the manifest on Android 8+. This replaces the previous
    // AppLockService, which was a non-foreground START_STICKY service and
    // was therefore being killed by the OS a few seconds after backgrounding
    // — silently disabling screen-off locking.
    private val screenLockReceiver = ScreenLockReceiver()
    private var screenReceiverRegistered = false

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

        // ---- Notification channel + schedule/cancel on settings change ----
        NotificationHelper.ensureChannel(this)
        appScope.launch {
            settingsRepo.settings.collect { s: AppSettings ->
                if (s.notificationsEnabled) {
                    NotificationScheduler.schedule(this@AlamalApp)
                } else {
                    NotificationScheduler.cancel(this@AlamalApp)
                }
            }
        }

        // ---- Foreground / background tracking ----
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                startedActivities++
                if (startedActivities == 1) {
                    val elapsed = if (backgroundedAtMs > 0L) {
                        System.currentTimeMillis() - backgroundedAtMs
                    } else {
                        0L
                    }
                    backgroundedAtMs = 0L
                    pendingLockJob?.cancel()
                    pendingLockJob = null
                    if (elapsed > BACKGROUND_LOCK_DELAY_MS && AppLockController.isEnabled()) {
                        AppLockController.lock()
                    }
                }
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities--
                if (startedActivities <= 0) {
                    startedActivities = 0
                    if (AppLockController.isEnabled()) {
                        backgroundedAtMs = System.currentTimeMillis()
                        pendingLockJob?.cancel()
                        pendingLockJob = appScope.launch {
                            delay(BACKGROUND_LOCK_DELAY_MS)
                            if (backgroundedAtMs > 0L) {
                                AppLockController.lock()
                            }
                        }
                    }
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        // ---- Lock the app as soon as the screen turns off ----
        // Registered here (Application context) instead of in a Service, so
        // no Service needs to be started. Kept alive as long as the process
        // is alive, which is exactly what we need for this receiver.
        registerScreenLockReceiver()
    }

    private fun registerScreenLockReceiver() {
        if (screenReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(
                    screenLockReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED,
                )
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(screenLockReceiver, filter)
            }
            screenReceiverRegistered = true
        } catch (_: Throwable) {
            // Non-fatal — backgrounding lock still works via the activity
            // lifecycle callbacks above.
        }
    }

    private companion object {
        const val AUTO_BACKUP_DEBOUNCE_MS = 3_000L
        const val BACKGROUND_LOCK_DELAY_MS = 30_000L
    }
}
