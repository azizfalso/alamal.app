package com.alamal.app

import android.app.Application
import com.alamal.app.data.AppRepository
import com.alamal.app.data.backup.BackupCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class AlamalApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()

        val repo = AppRepository.get(this)
        val coordinator = BackupCoordinator(this)

        // Debounce rapid changes: only run one auto backup
        // after 3 seconds of inactivity following the last change.
        appScope.launch {
            repo.dataChanged
                .debounce(AUTO_BACKUP_DEBOUNCE_MS)
                .collect {
                    runCatching { coordinator.performAutoBackupIfEnabled() }
                }
        }
    }

    private companion object {
        const val AUTO_BACKUP_DEBOUNCE_MS = 3_000L
    }
}
