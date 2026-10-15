package com.alamal.app.data.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
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
 *   1. SAF tree URI (chosen by the user in Settings) — most flexible.
 *   2. MediaStore Documents/Alamal — default for Android 10+.
 *
 * All DB-touching functions are suspend — call them from a coroutine.
 */
class BackupManager(private val context: Context) {

    private val db = AppDatabase.get(context)

    suspend fun buildPayload(): BackupPayload {
        val workDays = db.workDayDao().getAllForBackup()
        val allBreaks = workDays.flatMap { db.breakDao().getForDay(it.id) }
        val remotes = db.remoteWorkDao().getAllForBackup()
        val credits = db.creditDao().getAllForBackup()
        val periods = db.periodDao().getAll()

        // Save user preferences with the backup, but deliberately EXCLUDE:
        //   - pin:                 security-sensitive, never leaves the device
        //   - backupEncrypted:     whether encryption is on; restored blindly
        //                          could confuse the user, and the password
        //                          is not part of the backup anyway
        //   - backupTreeUri:       path-specific, meaningless on another device
        //   - lastNotifiedDayId:   internal state, not a user preference
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
                return WriteResult.Failure("كلمة مرور التشفير مطلوبة")
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
            WriteResult.Failure(t.message ?: "خطأ غير معروف")
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
            ?: return WriteResult.Failure("تعذّر إنشاء الملف في مجلد Documents/Alamal")

        try {
            resolver.openOutputStream(uri)?.use { os -> os.write(bytes) }
                ?: return WriteResult.Failure("تعذّر فتح الملف للكتابة")
        } catch (t: Throwable) {
            return WriteResult.Failure("فشل الكتابة: ${t.message}")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        }

        return WriteResult.Success(uri = uri, displayName = fileName, sizeBytes = bytes.size)
    }

    private fun writeToSaf(treeUri: Uri, fileName: String, bytes: ByteArray): WriteResult {
        val dir = DocumentFile.fromTreeUri(context, treeUri)
            ?: return WriteResult.Failure("مجلد SAF غير متاح")
