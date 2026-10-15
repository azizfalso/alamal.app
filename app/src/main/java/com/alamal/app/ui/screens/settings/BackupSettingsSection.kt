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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.alamal.app.R
import com.alamal.app.data.backup.BackupManager
import com.alamal.app.data.backup.BackupPayload
import com.alamal.app.data.backup.BackupPasswordHolder
import com.alamal.app.data.prefs.AppSettings
import com.alamal.app.data.prefs.SettingsRepository
import com.alamal.app.ui.components.AlamalCard
import com.alamal.app.ui.components.AlamalDivider
import com.alamal.app.ui.theme.LocalExtendedColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupSettingsSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepo = remember { SettingsRepository(context) }
    val settings by settingsRepo.settings.collectAsState(initial = AppSettings.DEFAULT)
    val manager = remember { BackupManager(context) }

    // Pre-resolve all strings outside of composable lambdas to avoid
    // accidental reads inside non-composable callbacks.
    val stringEnterPasswordFirst = stringResource(R.string.backup_dialog_enter_password_first)
    val stringGenericError = stringResource(R.string.backup_generic_error)
    val stringRestoreSuccess = stringResource(R.string.backup_restore_success)
    val stringFolderPermissionError = stringResource(R.string.backup_folder_permission_error)
    val stringNoAutoYet = stringResource(R.string.backup_no_auto_yet)
    val stringEncryptionEnabled = stringResource(R.string.backup_encryption_enabled_session)
    val stringFolderChosen = stringResource(R.string.backup_folder_chosen)
    val stringSavedToastFmt = stringResource(R.string.backup_saved_toast)

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
            toast(stringEnterPasswordFirst)
            return
        }
        busy = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val payload = manager.buildPayload()
                    val treeUri = settings.backupTreeUri
                        .takeIf { it.isNotEmpty() }
                        ?.let { runCatching { Uri.parse(it) }.getOrNull() }
                    manager.writeBackup(
                        payload = payload,
                        manual = manual,
                        encrypted = settings.backupEncrypted,
                        password = BackupPasswordHolder.get(),
                        safTreeUri = treeUri,
                    )
                }
                when (result) {
                    is BackupManager.WriteResult.Success ->
                        toast(String.format(
                            stringSavedToastFmt,
                            result.displayName,
                            result.sizeBytes / 1024,
                        ))
                    is BackupManager.WriteResult.Failure -> toast(result.message)
                }
            } catch (t: Throwable) {
                toast(String.format(stringGenericError, t.message ?: ""))
            } finally {
                busy = false
            }
        }
    }

    fun tryReadForRestore(uri: Uri, password: String?) {
        busy = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    manager.readBackup(uri, password)
                }
                when (result) {
                    is BackupManager.RestoreResult.Success -> {
                        pendingRestoreUri = uri
                        pendingRestorePayload = result.payload
                        showRestoreConfirm = true
                    }
                    is BackupManager.RestoreResult.Failure -> {
                        toast(result.message)
                        showPasswordForRestore = false
                    }
                }
            } catch (t: Throwable) {
                toast(String.format(stringGenericError, t.message ?: ""))
            } finally {
                busy = false
            }
        }
    }

    fun performRestore(payload: BackupPayload) {
        busy = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    manager.restoreIntoDatabase(payload)
                }
                when (result) {
                    is BackupManager.RestoreResult.Success -> toast(stringRestoreSuccess)
                    is BackupManager.RestoreResult.Failure -> toast(result.message)
                }
            } catch (t: Throwable) {
                toast(String.format(stringGenericError, t.message ?: ""))
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
                toast(String.format(stringFolderChosen, uri.lastPathSegment ?: ""))
            } catch (t: Throwable) {
                toast(String.format(stringFolderPermissionError, t.message ?: ""))
            }
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) startRestoreFlow(uri)
    }

    val autoBackupPaused = settings.backupEncrypted && !BackupPasswordHolder.has()

    AlamalCard {
        Text(
            text = stringResource(R.string.backup_section_title),
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
                    text = stringResource(R.string.backup_auto_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = when {
                        autoBackupPaused && settings.autoBackupEnabled ->
                            stringResource(R.string.backup_auto_paused_encrypted)
                        settings.autoBackupEnabled ->
                            stringResource(R.string.backup_auto_subtitle)
                        else ->
                            stringResource(R.string.backup_auto_disabled)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (autoBackupPaused && settings.autoBackupEnabled)
                        MaterialTheme.colorScheme.error
                    else
                        LocalExtendedColors.current.muted,
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
                    text = stringResource(R.string.backup_encrypt_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = when {
                        settings.backupEncrypted && BackupPasswordHolder.has() ->
                            stringResource(R.string.backup_encrypt_on_session)
                        settings.backupEncrypted ->
                            stringResource(R.string.backup_encrypt_on_no_session)
                        else -> stringResource(R.string.backup_encrypt_off)
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
            ) { Text(stringResource(R.string.backup_enter_password_for_session)) }
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
            ) { Text(stringResource(R.string.backup_choose_folder)) }
            if (settings.backupTreeUri.isNotEmpty()) {
                OutlinedButton(
                    onClick = { scope.launch { settingsRepo.setBackupTreeUri("") } },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text(stringResource(R.string.backup_clear)) }
            }
        }
        Text(
            text = if (settings.backupTreeUri.isEmpty())
                stringResource(R.string.backup_default_folder)
            else stringResource(R.string.backup_chosen_folder, settings.backupTreeUri),
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
        ) { Text(stringResource(R.string.backup_new_now)) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val treeUri = settings.backupTreeUri
                            .takeIf { it.isNotEmpty() }
                            ?.let { runCatching { Uri.parse(it) }.getOrNull() }
                        val uri = withContext(Dispatchers.IO) {
                            manager.findAutoBackup(treeUri)
                        }
                        if (uri == null) toast(stringNoAutoYet)
                        else startRestoreFlow(uri)
                    }
                },
                enabled = !busy,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text(stringResource(R.string.backup_from_auto)) }

            OutlinedButton(
                onClick = {
                    fileLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                },
                enabled = !busy,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text(stringResource(R.string.backup_from_file)) }
        }

        Text(
            text = stringResource(R.string.backup_restore_note),
            style = MaterialTheme.typography.bodySmall,
            color = LocalExtendedColors.current.muted,
        )
    }

    if (showEncryptionDialog) {
        PasswordDialog(
            title = stringResource(R.string.backup_password_dialog_title_set),
            subtitle = stringResource(R.string.backup_password_dialog_subtitle_set),
            requireConfirm = true,
            onDismiss = { showEncryptionDialog = false },
            onConfirm = { pwd ->
                showEncryptionDialog = false
                BackupPasswordHolder.set(pwd)
                scope.launch { settingsRepo.setBackupEncrypted(true) }
                toast(stringEncryptionEnabled)
            },
        )
    }

    if (showPasswordForRestore && pendingRestoreUri != null) {
        PasswordDialog(
            title = stringResource(R.string.backup_password_dialog_title_restore),
            subtitle = stringResource(R.string.backup_password_dialog_subtitle_restore),
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
            title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.backup_restore_confirm_body,
                        p.workDays.size,
                        p.breaks.size,
                        p.remoteWorks.size,
                        p.credits.size,
                        p.periods.size,
                        if (p.settingsMap.isEmpty())
                            stringResource(R.string.backup_restore_settings_no)
                        else
                            stringResource(R.string.backup_restore_settings_yes),
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        val payload = pendingRestorePayload
                        pendingRestorePayload = null
                        pendingRestoreUri = null
                        if (payload != null) performRestore(payload)
                    },
                ) { Text(stringResource(R.string.backup_restore_confirm_action)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        pendingRestorePayload = null
                        pendingRestoreUri = null
                    },
                ) { Text(stringResource(R.string.cancel)) }
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

    // Pre-resolve the error strings so we can use them in callbacks.
    val errTooShort = stringResource(R.string.backup_password_too_short)
    val errNoMatch = stringResource(R.string.backup_passwords_do_not_match)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { pwd = it; error = null },
                    label = { Text(stringResource(R.string.backup_password_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (requireConfirm) {
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it; error = null },
                        label = { Text(stringResource(R.string.backup_password_confirm_label)) },
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
                        pwd.length < 4 -> error = errTooShort
                        requireConfirm && pwd != confirm -> error = errNoMatch
                        else -> onConfirm(pwd)
                    }
                },
            ) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
