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
                toast(String.format(stringGenericError, t.message ?
