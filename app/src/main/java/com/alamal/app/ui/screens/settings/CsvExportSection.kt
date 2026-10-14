package com.alamal.app.ui.screens.settings

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alamal.app.data.prefs.AppSettings
import com.alamal.app.data.prefs.SettingsRepository
import com.alamal.app.ui.components.AlamalCard
import com.alamal.app.ui.components.AlamalDivider
import com.alamal.app.ui.theme.LocalExtendedColors
import com.alamal.app.util.CsvExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CsvExportSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepo = remember { SettingsRepository(context) }
    val settings by settingsRepo.settings.collectAsState(initial = AppSettings.DEFAULT)
    val exporter = remember { CsvExporter(context) }
    var busy by remember { mutableStateOf(false) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    fun runExport(all: Boolean) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val treeUri = settings.backupTreeUri
                    .takeIf { it.isNotEmpty() }
                    ?.let { runCatching { Uri.parse(it) }.getOrNull() }
                val result = withContext(Dispatchers.IO) {
                    if (all) exporter.exportAll(treeUri, settings.currency)
                    else exporter.exportCurrentPeriod(treeUri, settings.currency)
                }
                when (result) {
                    is CsvExporter.Result.Success ->
                        toast("تم حفظ ${result.displayName} (${result.sizeBytes / 1024} ك.ب)")
                    is CsvExporter.Result.Failure -> toast(result.message)
                }
            } catch (t: Throwable) {
                toast("خطأ: ${t.message}")
            } finally {
                busy = false
            }
        }
    }

    AlamalCard {
        Text(
            text = "تصدير CSV",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = LocalExtendedColors.current.accent,
        )
        AlamalDivider()

        Text(
            text = "ملف جدولي بسيط يمكن فتحه في Excel أو Google Sheets. " +
                "يُحفظ في نفس مجلد النسخ الاحتياطي.",
            style = MaterialTheme.typography.bodySmall,
            color = LocalExtendedColors.current.muted,
        )

        Button(
            onClick = { runExport(all = false) },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
        ) { Text("تصدير الفترة الحالية") }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { runExport(all = true) },
                enabled = !busy,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text("تصدير كل السجلات") }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = "الأعمدة: النوع، التاريخ، التفاصيل، المدة، المبلغ، ملاحظة.",
            style = MaterialTheme.typography.bodySmall,
            color = LocalExtendedColors.current.meta,
        )
    }
}
