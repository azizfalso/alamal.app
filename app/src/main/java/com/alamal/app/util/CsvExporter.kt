package com.alamal.app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.alamal.app.data.db.AppDatabase
import com.alamal.app.domain.MoneyCalculator

/**
 * Builds a CSV file for the app's data.
 *
 * File structure (UTF-8 with BOM so Excel opens Arabic correctly):
 *   النوع,التاريخ,التفاصيل,المدة (دقيقة),المبلغ,ملاحظة
 *
 * Row types:
 *   - "عمل"     (work day)
 *   - "عن بُعد"  (remote work)
 *   - "كريدي"   (credit, negative amount)
 */
class CsvExporter(private val context: Context) {

    private val db = AppDatabase.get(context)

    sealed class Result {
        data class Success(val uri: Uri, val displayName: String, val sizeBytes: Int) : Result()
        data class Failure(val message: String) : Result()
    }

    /** Exports only the current (open) period. */
    suspend fun exportCurrentPeriod(safTreeUri: Uri?, currency: String): Result {
        val workDays = db.workDayDao().getCurrent()
        val remotes = db.remoteWorkDao().getCurrent()
        val credits = db.creditDao().getCurrent()
        val content = buildCsv(workDays, remotes, credits, currency)
        val fileName = "alamal_current_${timestamp()}.csv"
        return writeCsv(content, fileName, safTreeUri)
    }

    /** Exports everything including archived periods. */
    suspend fun exportAll(safTreeUri: Uri?, currency: String): Result {
        val workDays = db.workDayDao().getAllForBackup()
        val remotes = db.remoteWorkDao().getAllForBackup()
        val credits = db.creditDao().getAllForBackup()
        val content = buildCsv(workDays, remotes, credits, currency)
        val fileName = "alamal_all_${timestamp()}.csv"
        return writeCsv(content, fileName, safTreeUri)
    }

    private suspend fun buildCsv(
        workDays: List<com.alamal.app.data.db.entity.WorkDayEntity>,
        remotes: List<com.alamal.app.data.db.entity.RemoteWorkEntity>,
        credits: List<com.alamal.app.data.db.entity.CreditEntity>,
        currency: String,
    ): String {
        val sb = StringBuilder()
        // BOM to make Excel happy with Arabic.
        sb.append('\uFEFF')
        // Header
        sb.append("النوع,التاريخ,التفاصيل,المدة (دقيقة),المبلغ (").append(currency).append("),ملاحظة\n")

        // Work days sorted by date ascending.
        workDays.sortedBy { it.clockIn }.forEach { day ->
            val brk = db.breakDao().getForDay(day.id)
            val stats = MoneyCalculator.computeDayStats(day, brk)
            val date = DateFormatters.dateIso(day.clockIn)
            val inTime = DateFormatters.time(day.clockIn)
            val outTime = day.clockOut?.let { DateFormatters.time(it) } ?: "—"
            val details = "$inTime → $outTime"
            val note = day.note ?: ""
            sb.append(escape("عمل")).append(',')
                .append(escape(date)).append(',')
                .append(escape(details)).append(',')
                .append(stats.actualMinutes).append(',')
                .append(String.format(java.util.Locale.US, "%.2f", stats.wage)).append(',')
                .append(escape(note)).append('\n')
        }

        remotes.sortedBy { it.date }.forEach { r ->
            val date = DateFormatters.dateIso(r.date)
            val note = r.note ?: ""
            sb.append(escape("عن بُعد")).append(',')
                .append(escape(date)).append(',')
                .append(escape("")).append(',')
                .append(escape("")).append(',')
                .append(String.format(java.util.Locale.US, "%.2f", r.amount)).append(',')
                .append(escape(note)).append('\n')
        }

        credits.sortedBy { it.date }.forEach { c ->
            val date = DateFormatters.dateIso(c.date)
            val note = c.note ?: ""
            sb.append(escape("كريدي")).append(',')
                .append(escape(date)).append(',')
                .append(escape("")).append(',')
                .append(escape("")).append(',')
                .append(String.format(java.util.Locale.US, "%.2f", -c.amount)).append(',')
                .append(escape(note)).append('\n')
        }

        return sb.toString()
    }

    private fun writeCsv(content: String, fileName: String, safTreeUri: Uri?): Result {
        val bytes = content.toByteArray(Charsets.UTF_8)
        return try {
            if (safTreeUri != null) {
                writeToSaf(safTreeUri, fileName, bytes)
            } else {
                writeToMediaStore(fileName, bytes)
            }
        } catch (t: Throwable) {
            Result.Failure(t.message ?: "خطأ غير معروف")
        }
    }

    private fun writeToSaf(treeUri: Uri, fileName: String, bytes: ByteArray): Result {
        val dir = DocumentFile.fromTreeUri(context, treeUri)
            ?: return Result.Failure("مجلد SAF غير متاح")
        if (!dir.canWrite()) {
            return Result.Failure("لا توجد صلاحية كتابة في المجلد المختار")
        }
        dir.findFile(fileName)?.delete()
        val file = dir.createFile("text/csv", fileName)
            ?: return Result.Failure("تعذّر إنشاء الملف في المجلد المختار")
        try {
            context.contentResolver.openOutputStream(file.uri)?.use { os -> os.write(bytes) }
                ?: return Result.Failure("تعذّر فتح الملف في مجلد SAF")
        } catch (t: Throwable) {
            return Result.Failure("فشل الكتابة: ${t.message}")
        }
        return Result.Success(uri = file.uri, displayName = fileName, sizeBytes = bytes.size)
    }

    private fun writeToMediaStore(fileName: String, bytes: ByteArray): Result {
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/Alamal")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(collection, values)
            ?: return Result.Failure("تعذّر إنشاء الملف في Documents/Alamal")

        try {
            resolver.openOutputStream(uri)?.use { os -> os.write(bytes) }
                ?: return Result.Failure("تعذّر فتح الملف للكتابة")
        } catch (t: Throwable) {
            return Result.Failure("فشل الكتابة: ${t.message}")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        }

        return Result.Success(uri = uri, displayName = fileName, sizeBytes = bytes.size)
    }

    /** Wraps a value in double quotes and escapes inner quotes. */
    private fun escape(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') ||
            value.contains('\n') || value.contains('\r')
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    private fun timestamp(): String {
        val f = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.US)
        return f.format(java.util.Date())
    }
}
