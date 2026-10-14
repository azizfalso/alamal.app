package com.alamal.app

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    // App is back in foreground → cancel pending lock.
                    pendingLockJob?.cancel()
                    pendingLockJob = null
                }

                override fun onStop(owner: LifecycleOwner) {
                    // App went to background → schedule a lock after 30s.
                    if (!AppLockController.isEnabled()) return
                    pendingLockJob?.cancel()
                    pendingLockJob = appScope.launch {
                        delay(BACKGROUND_LOCK_DELAY_MS)
                        AppLockController.lock()
                    }
                }
            }
        )

        // ---- Best-effort: catch swipe-away from Recents + screen off ----
        try {
            val intent = Intent(this, AppLockService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startService(intent)
            } else {
                startService(intent)
            }
        } catch (_: Throwable) {
            // Non-fatal — lock still works via ProcessLifecycleOwner.
        }
    }

    private companion object {
        const val AUTO_BACKUP_DEBOUNCE_MS = 3_000L
        const val BACKGROUND_LOCK_DELAY_MS = 30_000L
    }
}
