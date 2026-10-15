package com.alamal.app.data.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.alamal.app.R
import com.alamal.app.data.db.AppDatabase
import com.alamal.app.data.prefs.SettingsRepository
import com.alamal.app.data.prefs.ThemeMode
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles writing/reading backup files.
 *
 * Destinations (in priority order):
 *   1. SAF tree URI (chosen by the user in Settings) - most flexible.
 *   2. MediaStore Documents/Alamal - default for Android 10+.
 *
 * All DB-touching functions are suspend - call them from a coroutine.
 *
 * NOTE ON STRING ENCODING:
 *   Every user-facing message is a string resource (R.string.*), never an
 *   inline Arabic literal. Arabic text mixed directly into Kotlin source
 *   occasionally picks up invisible bidi marks or smart quotes during
 *   editing, and those break the Kotlin parser with confusing "Expecting '}'"
 *   errors far away from the real line. Pulling all Arabic into strings.xml
 *   makes that class of bug impossible.
 */
class BackupManager(private val context: Context) {

    private val db = AppDatabase.get(context)

    suspend fun buildPayload(): BackupPayload {
        val workDays = db.workDayDao().getAllForBackup()
        val allBreaks = workDays.flatMap { db.breakDao().getForDay(it.id) }
        val remotes = db.remoteWorkDao().getAllForBackup()
        val credits = db.creditDao().getAllForBackup()
        val periods = db.periodDao().getAll()

        // Persist user preferences with the backup, but deliberately EXCLUDE:
        //   - pin                 (security-sensitive, never leaves the device)
        //   - backupEncrypted     (restored blindly could confuse the user)
        //   - backupTreeUri       (path-specific, meaningless on another device)
        //   - lastNotifiedDayId   (internal state, not a user preference)
        val settings = SettingsRepository(context).settings.first()
        val settingsMap = mapOf(
            "themeMode" to settings.themeMode.key,
            "appColorHex" to settings.appColorHex,
            "currency" to settings.currency,
            "hourlyRate" to settings.hourlyRate.toString(),
            "notificationsEnabled" to settings.notificationsEnabled.toString(),
            "longShiftHours" to settings.longShiftHours.toString(),
            "autoBackupEnabled" to settings.autoBackupEnabled.toString(),
        )

        return BackupPayload(
            workDays = workDays,
            breaks = allBreaks,
            remoteWorks = remotes,
            credits = credits,
            periods = periods,
            settingsMap = settingsMap,
        )
    }

    fun serialize(payload: BackupPayload): ByteArray {
        return payload.toJson().toString(2).toByteArray(Charsets.UTF_8)
    }

    fun deserialize(bytes: ByteArray): BackupPayload {
        val text = bytes.toString(Charsets.UTF_8)
        return BackupPayload.fromJson(JSONObject(text))
    }

    sealed class WriteResult {
        data class Success(val uri: Uri, val displayName: String, val sizeBytes: Int) : WriteResult()
        data class Failure(val message: String) : WriteResult()
    }

    fun writeBackup(
        payload: BackupPayload,
        manual: Boolean,
        encrypted: Boolean,
        password: String?,
        safTreeUri: Uri?,
    ): WriteResult {
        val plain = serialize(payload)
        val finalBytes: ByteArray = if (encrypted) {
            if (password.isNullOrEmpty()) {
                return WriteResult.Failure(context.getString(R.string.backup_password_required))
            }
            CryptoHelper.encrypt(plain, password)
        } else plain

        val fileName = buildFileName(manual, encrypted)

        return try {
            if (safTreeUri != null) {
                writeToSaf(safTreeUri, fileName, finalBytes)
            } else {
                writeToMediaStore(fileName, finalBytes)
            }
        } catch (t: Throwable) {
            WriteResult.Failure(t.message ?: context.getString(R.string.backup_unknown_error))
        }
    }

    private fun buildFileName(manual: Boolean, encrypted: Boolean): String {
        val ext = if (encrypted) "enc" else "json"
        return if (manual) {
            val ts = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            "alamal_$ts.$ext"
        } else {
            "alamal_auto.$ext"
        }
    }

    private fun writeToMediaStore(fileName: String, bytes: ByteArray): WriteResult {
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        try {
            val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME)
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
            resolver.query(collection, projection, selection, arrayOf(fileName), null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val uri = Uri.withAppendedPath(collection, id.toString())
                    resolver.delete(uri, null, null)
                }
            }
        } catch (_: Throwable) {
            // Non-fatal.
        }

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType(fileName))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/Alamal")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(collection, values)
            ?: return WriteResult.Failure(context.getString(R.string.backup_cannot_create_in_documents))

        try {
            resolver.openOutputStream(uri)?.use { os -> os.write(bytes) }
                ?: return WriteResult.Failure(context.getString(R.string.backup_cannot_open_for_write))
        } catch (t: Throwable) {
            return WriteResult.Failure(
                context.getString(R.string.backup_write_failed, t.message ?: "")
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        }

        return WriteResult.Success(uri = uri, displayName = fileName, sizeBytes = bytes.size)
    }

    private fun writeToSaf(treeUri: Uri, fileName: String, bytes: ByteArray): WriteResult {
        val dir = DocumentFile.fromTreeUri(context, treeUri)
            ?: return WriteResult.Failure(context.getString(R.string.backup_saf_folder_unavailable))
        if (!dir.canWrite()) {
            return WriteResult.Failure(context.getString(R.string.backup_saf_no_write_permission))
        }

        dir.findFile(fileName)?.delete()

        val file = dir.createFile(mimeType(fileName), fileName)
            ?: return WriteResult.Failure(context.getString(R.string.backup_saf_cannot_create))

        try {
            context.contentResolver.openOutputStream(file.uri)?.use { os -> os.write(bytes) }
                ?: return WriteResult.Failure(context.getString(R.string.backup_saf_cannot_open))
        } catch (t: Throwable) {
            return WriteResult.Failure(
                context.getString(R.string.backup_saf_write_failed, t.message ?: "")
            )
        }

        return WriteResult.Success(uri = file.uri, displayName = fileName, sizeBytes = bytes.size)
    }

    private fun mimeType(fileName: String): String =
        if (fileName.endsWith(".enc")) "application/octet-stream" else "application/json"

    sealed class RestoreResult {
        data class Success(val payload: BackupPayload) : RestoreResult()
        data class Failure(val message: String) : RestoreResult()
    }

    fun readBackup(uri: Uri, password: String?): RestoreResult {
        val bytes: ByteArray = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return RestoreResult.Failure(context.getString(R.string.backup_cannot_open))
        } catch (t: Throwable) {
            return RestoreResult.Failure(
                context.getString(R.string.backup_cannot_read, t.message ?: "")
            )
        }

        val plain = if (CryptoHelper.isEncrypted(bytes)) {
            if (password.isNullOrEmpty()) {
                return RestoreResult.Failure(
                    context.getString(R.string.backup_file_encrypted_need_password)
                )
            }
            try {
                CryptoHelper.decrypt(bytes, password)
            } catch (t: Throwable) {
                return RestoreResult.Failure(context.getString(R.string.backup_decrypt_failed))
            }
        } else bytes

        return try {
            RestoreResult.Success(deserialize(plain))
        } catch (t: Throwable) {
            RestoreResult.Failure(
                context.getString(R.string.backup_invalid_file, t.message ?: "")
            )
        }
    }

    suspend fun restoreIntoDatabase(payload: BackupPayload): RestoreResult {
        return try {
            db.clearAllTables()

            payload.periods.forEach { p -> db.periodDao().insert(p) }
            payload.workDays.forEach { d -> db.workDayDao().insert(d) }
            payload.breaks.forEach { b -> db.breakDao().insert(b) }
            payload.remoteWorks.forEach { r -> db.remoteWorkDao().insert(r) }
            payload.credits.forEach { c -> db.creditDao().insert(c) }

            // Apply the user preferences that were saved with the backup.
            // Security-sensitive values (PIN, encryption password, SAF tree
            // URI) are intentionally NOT restored - see buildPayload().
            applySettingsFromBackup(payload.settingsMap)

            RestoreResult.Success(payload)
        } catch (t: Throwable) {
            RestoreResult.Failure(
                context.getString(R.string.backup_restore_failed, t.message ?: "")
            )
        }
    }

    private suspend fun applySettingsFromBackup(map: Map<String, String>) {
        if (map.isEmpty()) return
        val repo = SettingsRepository(context)
        map["themeMode"]?.let { repo.setThemeMode(ThemeMode.fromKey(it)) }
        map["appColorHex"]?.let { repo.setAppColorHex(it) }
        map["currency"]?.let { repo.setCurrency(it) }
        map["hourlyRate"]?.toDoubleOrNull()?.let { repo.setHourlyRate(it) }
        map["notificationsEnabled"]?.toBoolean()?.let { repo.setNotificationsEnabled(it) }
        map["longShiftHours"]?.toIntOrNull()?.let { repo.setLongShiftHours(it) }
        map["autoBackupEnabled"]?.toBoolean()?.let { repo.setAutoBackupEnabled(it) }
    }

    fun findAutoBackup(safTreeUri: Uri?): Uri? {
        if (safTreeUri != null) {
            val dir = DocumentFile.fromTreeUri(context, safTreeUri) ?: return null
            dir.findFile("alamal_auto.enc")?.let { return it.uri }
            dir.findFile("alamal_auto.json")?.let { return it.uri }
            return null
        }
        val names = listOf("alamal_auto.enc", "alamal_auto.json")
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }
        for (name in names) {
            val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME)
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
            try {
                context.contentResolver.query(
                    collection, projection, selection, arrayOf(name), null
                )?.use { c ->
                    if (c.moveToFirst()) {
                        val id = c.getLong(0)
                        return Uri.withAppendedPath(collection, id.toString())
                    }
                }
            } catch (_: Throwable) {
                // Try next name.
            }
        }
        return null
    }
}
