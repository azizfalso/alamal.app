package com.alamal.app.data.backup

import android.content.Context
import android.net.Uri
import com.alamal.app.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Keeps the encryption password in memory for the current session only.
 * Not persisted on disk (by design — safer).
 */
object BackupPasswordHolder {
    @Volatile
    private var current: String? = null

    fun set(p: String?) {
        current = p?.takeIf { it.isNotEmpty() }
    }

    fun has(): Boolean = !current.isNullOrEmpty()

    fun get(): String? = current
}

/**
 * Runs auto backup on demand. Reads the current settings to know
 * whether auto is enabled and whether encryption is required.
 */
class BackupCoordinator(context: Context) {

    private val appContext = context.applicationContext
    private val manager = BackupManager(appContext)
    private val settingsRepo = SettingsRepository(appContext)

    /**
     * Runs the automatic backup if enabled in settings.
     *
     * When encryption is ON, this requires a password in memory
     * (set by the user in Settings during this app session).
     * Otherwise it silently returns null and does nothing.
     */
    suspend fun performAutoBackupIfEnabled(): BackupManager.WriteResult? {
        val settings = settingsRepo.settings.first()
        if (!settings.autoBackupEnabled) return null

        val password = if (settings.backupEncrypted) {
            BackupPasswordHolder.get() ?: return null
        } else null

        val payload = manager.buildPayload()
        val treeUri = settings.backupTreeUri
            .takeIf { it.isNotEmpty() }
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }

        return manager.writeBackup(
            payload = payload,
            manual = false,
            encrypted = settings.backupEncrypted,
            password = password,
            safTreeUri = treeUri,
        )
    }
}
