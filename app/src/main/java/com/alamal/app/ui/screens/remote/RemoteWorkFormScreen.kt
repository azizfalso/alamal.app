package com.alamal.app.ui.screens.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.alamal.app.R
import com.alamal.app.data.AppRepository
import com.alamal.app.data.db.entity.RemoteWorkEntity
import com.alamal.app.data.prefs.AppSettings
import com.alamal.app.data.prefs.SettingsRepository
import com.alamal.app.ui.components.AlamalCard
import com.alamal.app.ui.components.AlamalDivider
import com.alamal.app.ui.theme.LocalExtendedColors
import com.alamal.app.util.DateFormatters
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteWorkFormScreen(
    itemId: Long,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { AppRepository.get(context) }
    val settingsRepo = remember { SettingsRepository(context) }
    val settings by settingsRepo.settings.collectAsState(initial = AppSettings.DEFAULT)

    var loaded by remember { mutableStateOf(false) }
    var currentId by remember { mutableStateOf(if (itemId > 0L) itemId else 0L) }
    var dateMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var editingDate by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        if (loaded) return@LaunchedEffect
        if (itemId > 0L) {
            val item = repo.getRemoteWork(itemId)
            if (item != null) {
                currentId = item.id
                dateMs = item.date
                amountText = formatAmount(item.amount)
                noteText = item.note.orEmpty()
            }
        }
        loaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (currentId == 0L) "إضافة عمل عن بُعد" else "تعديل عمل عن بُعد")
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    if (currentId > 0L) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "حذف",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AlamalCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "التاريخ",
                            style = MaterialTheme.typography.labelMedium,
                            color = LocalExtendedColors.current.muted,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = DateFormatters.dateArabic(dateMs),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    TextButton(onClick = { editingDate = true }) { Text("تغيير") }
                }
            }

            AlamalCard {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ (${settings.currency})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("ملاحظة (اختياري)") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDone,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text(stringResource(R.string.cancel)) }

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        if (amount == null || amount <= 0.0) return@Button
                        scope.launch {
                            repo.saveRemoteWork(
                                RemoteWorkEntity(
                                    id = currentId,
                                    periodId = null,
                                    date = dateMs,
                                    amount = amount,
                                    note = noteText.ifBlank { null },
                                )
                            )
                            onDone()
                        }
                    },
                    enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text(stringResource(R.string.save)) }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (editingDate) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = dateMs)
        DatePickerDialog(
            onDismissRequest = { editingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    val picked = dateState.selectedDateMillis
                    if (picked != null) {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = picked
                            set(Calendar.HOUR_OF_DAY, 12)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        dateMs = cal.timeInMillis
                    }
                    editingDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { editingDate = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف السجل") },
            text = { Text("سيتم حذف هذا السجل. ستحصل على 10 ثوانٍ للتراجع.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        scope.launch {
                            val item = repo.getRemoteWork(currentId)
                            if (item != null) repo.deleteRemoteWorkWithUndo(item)
                            onDone()
                        }
                    },
                ) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("إلغاء") }
            },
        )
    }
}

private fun formatAmount(a: Double): String =
    if (a == a.toLong().toDouble()) a.toLong().toString() else a.toString()
