package com.alamal.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.alamal.app.data.backup.BackupManager
import com.alamal.app.data.backup.BackupPayload
import com.alamal.app.data.backup.BackupPasswordHolder
import com.alamal.app.data.prefs.AppSettings
import com.alamal.app.data.prefs.SettingsRepository
import com.alamal.app.ui.components.AlamalCard
import com.alamal.app.ui.components.AlamalDivider
import com.alamal.app.ui.theme.LocalExtendedColors
import kotlinx.coroutines.launch

@Composable
fun BackupSettingsSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepo = remember { SettingsRepository(context) }
    val settings by settingsRepo.settings.collectAsState(initial = AppSettings.DEFAULT)
    val manager = remember { BackupManager(context) }

    var showEncryptionDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var pendingRestorePayload by remember { mutableStateOf<BackupPayload?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showPasswordForRestore by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    fun peekEncrypted(uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val header = ByteArray(4)
                input.read(header) == 4 &&
                    header[0] == 0x41.toByte() && header[1] == 0x4C.toByte() &&
                    header[2] == 0x41.toByte() && header[3] == 0x4D.toByte()
            } ?: false
        } catch (_: Throwable) { false }
    }

    fun performWrite(manual: Boolean) {
        if (busy) return
        if (settings.backupEncrypted && !BackupPasswordHolder.has()) {
            toast("أدخل كلمة مرور التشفير أولًا من القسم نفسه.")
            return
        }
        busy = true
        scope.launch {
            try {
                val payload = manager.buildPayload()
                val treeUri = settings.backupTreeUri
                    .takeIf { it.isNotEmpty() }
                    ?.let { runCatching { Uri.parse(it) }.getOrNull() }
                when (val res = manager.writeBackup(
                    payload = payload,
                    manual = manual,
                    encrypted = settings.backupEncrypted,
                    password = BackupPasswordHolder.get(),
                    safTreeUri = treeUri,
                )) {
                    is BackupManager.WriteResult.Success ->
                        toast("تم حفظ النسخة: ${res.displayName} (${res.sizeBytes / 1024} ك.ب)")
                    is BackupManager.WriteResult.Failure -> toast(res.message)
                }
            } finally {
                busy = false
            }
        }
    }

    fun tryReadForRestore(uri: Uri, password: String?) {
        busy = true
        scope.launch {
            try {
                when (val res = manager.readBackup(uri, password)) {
                    is BackupManager.RestoreResult.Success -> {
                        pendingRestoreUri = uri
                        pendingRestorePayload = res.payload
                        showRestoreConfirm = true
                    }
                    is BackupManager.RestoreResult.Failure -> {
                        toast(res.message)
                        showPasswordForRestore = false
                    }
                }
            } finally {
                busy = false
            }
        }
    }

    fun startRestoreFlow(uri: Uri) {
        if (peekEncrypted(uri) && !BackupPasswordHolder.has()) {
            pendingRestoreUri = uri
            showPasswordForRestore = true
        } else {
            tryReadForRestore(uri, BackupPasswordHolder.get())
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                scope.launch { settingsRepo.setBackupTreeUri(uri.toString()) }
                toast("تم اختيار المجلد: ${uri.lastPathSegment ?: ""}")
            } catch (t: Throwable) {
                toast("تعذّر الحصول على صلاحية المجلد: ${t.message}")
            }
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) startRestoreFlow(uri)
    }

    AlamalCard {
        Text(
            text = "النسخ الاحتياطي",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = LocalExtendedColors.current.accent,
        )
        AlamalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    scope.launch { settingsRepo.setAutoBackupEnabled(!settings.autoBackupEnabled) }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(
                checked = settings.autoBackupEnabled,
                onCheckedChange = {
                    scope.launch { settingsRepo.setAutoBackupEnabled(it) }
                },
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = "نسخة تلقائية بعد كل تعديل",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "يُستبدل ملف واحد فقط (alamal_auto) ولا يمس النسخ اليدوية",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalExtendedColors.current.muted,
                )
            }
        }

        AlamalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (settings.backupEncrypted) {
                        scope.launch { settingsRepo.setBackupEncrypted(false) }
                    } else {
                        showEncryptionDialog = true
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(
                checked = settings.backupEncrypted,
                onCheckedChange = { checked ->
                    if (checked) showEncryptionDialog = true
                    else scope.launch { settingsRepo.setBackupEncrypted(false) }
                },
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = "تشفير النسخ الاحتياطية",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = when {
                        settings.backupEncrypted && BackupPasswordHolder.has() ->
                            "مُفعَّل — كلمة المرور محفوظة لهذه الجلسة"
                        settings.backupEncrypted ->
                            "مُفعَّل — لكن كلمة المرور غير محفوظة حاليًا"
                        else -> "غير مُفعَّل"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalExtendedColors.current.muted,
                )
            }
        }

        if (settings.backupEncrypted && !BackupPasswordHolder.has()) {
            OutlinedButton(
                onClick = { showEncryptionDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text("إدخال كلمة مرور التشفير لهذه الجلسة") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { folderLauncher.launch(null) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text("اختيار مجلد الحفظ") }
            if (settings.backupTreeUri.isNotEmpty()) {
                OutlinedButton(
                    onClick = { scope.launch { settingsRepo.setBackupTreeUri("") } },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("مسح") }
            }
        }
        Text(
            text = if (settings.backupTreeUri.isEmpty())
                "المكان الافتراضي: Documents/Alamal"
            else "المجلد المختار: ${settings.backupTreeUri}",
            style = MaterialTheme.typography.bodySmall,
            color = LocalExtendedColors.current.muted,
        )

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = { performWrite(manual = true) },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
        ) { Text("نسخة احتياطية جديدة الآن") }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    val treeUri = settings.backupTreeUri
                        .takeIf { it.isNotEmpty() }
                        ?.let { runCatching { Uri.parse(it) }.getOrNull() }
                    val uri = manager.findAutoBackup(treeUri)
                    if (uri == null) toast("لا توجد نسخة تلقائية بعد.")
                    else startRestoreFlow(uri)
                },
                enabled = !busy,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text("استعادة من التلقائية") }

            OutlinedButton(
                onClick = {
                    fileLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                },
                enabled = !busy,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text("استعادة من ملف") }
        }

        Text(
            text = "الاستعادة تُلغي كل البيانات الحالية وتستبدلها بمحتوى النسخة. لا تشارك كلمة مرور التشفير مع أحد.",
            style = MaterialTheme.typography.bodySmall,
            color = LocalExtendedColors.current.muted,
        )
    }

    if (showEncryptionDialog) {
        PasswordDialog(
            title = "كلمة مرور التشفير",
            subtitle = "ستُطلب عند كل نسخة/استعادة. لا يمكن استعادتها إن نسيتها.",
            requireConfirm = true,
            onDismiss = { showEncryptionDialog = false },
            onConfirm = { pwd ->
                showEncryptionDialog = false
                BackupPasswordHolder.set(pwd)
                scope.launch { settingsRepo.setBackupEncrypted(true) }
                toast("تم تفعيل التشفير لهذه الجلسة.")
            },
        )
    }

    if (showPasswordForRestore && pendingRestoreUri != null) {
        PasswordDialog(
            title = "كلمة مرور فك التشفير",
            subtitle = "هذه النسخة مشفَّرة.",
            requireConfirm = false,
            onDismiss = {
                showPasswordForRestore = false
                pendingRestoreUri = null
            },
            onConfirm = { pwd ->
                val uri = pendingRestoreUri
                showPasswordForRestore = false
                if (uri != null) tryReadForRestore(uri, pwd)
            },
        )
    }

    if (showRestoreConfirm && pendingRestorePayload != null) {
        val p = pendingRestorePayload!!
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirm = false
                pendingRestorePayload = null
                pendingRestoreUri = null
            },
            title = { Text("تأكيد الاستعادة") },
            text = {
                Text(
                    "سيتم استبدال كل البيانات الحالية بمحتوى النسخة:\n\n" +
                        "• أيام العمل: ${p.workDays.size}\n" +
                        "• الاستراحات: ${p.breaks.size}\n" +
                        "• العمل عن بُعد: ${p.remoteWorks.size}\n" +
                        "• الكريدي: ${p.credits.size}\n" +
                        "• الفترات المؤرشفة: ${p.periods.size}\n\n" +
                        "لا يمكن التراجع بعد المتابعة."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        val payload = pendingRestorePayload
                        pendingRestorePayload = null
                        pendingRestoreUri = null
                        if (payload != null) {
                            busy = true
                            scope.launch {
                                try {
                                    when (val r = manager.restoreIntoDatabase(payload)) {
                                        is BackupManager.RestoreResult.Success -> toast("تمت الاستعادة بنجاح.")
                                        is BackupManager.RestoreResult.Failure -> toast(r.message)
                                    }
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    },
                ) { Text("استعادة واستبدال") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        pendingRestorePayload = null
                        pendingRestoreUri = null
                    },
                ) { Text("إلغاء") }
            },
        )
    }
}

@Composable
private fun PasswordDialog(
    title: String,
    subtitle: String,
    requireConfirm: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var pwd by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { pwd = it; error = null },
                    label = { Text("كلمة المرور") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (requireConfirm) {
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it; error = null },
                        label = { Text("تأكيد كلمة المرور") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        pwd.length < 4 -> error = "كلمة المرور قصيرة جدًا (4 أحرف على الأقل)"
                        requireConfirm && pwd != confirm -> error = "الكلمتان غير متطابقتين"
                        else -> onConfirm(pwd)
                    }
                },
            ) { Text("تأكيد") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}
