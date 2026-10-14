package com.alamal.app.ui.screens.workday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alamal.app.R
import com.alamal.app.data.AppRepository
import com.alamal.app.data.db.entity.BreakEntity
import com.alamal.app.data.db.entity.WorkDayEntity
import com.alamal.app.data.prefs.AppSettings
import com.alamal.app.data.prefs.SettingsRepository
import com.alamal.app.ui.components.AlamalCard
import com.alamal.app.ui.components.AlamalDivider
import com.alamal.app.ui.components.AlamalStatRow
import com.alamal.app.ui.components.DateTimeWheelDialog
import com.alamal.app.ui.theme.LocalExtendedColors
import com.alamal.app.util.ArabicPlural
import com.alamal.app.util.DateFormatters
import com.alamal.app.util.MoneyFormat
import kotlinx.coroutines.launch

private data class BreakDraft(
    val id: Long = 0L,
    val startMs: Long,
    val endMs: Long,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkDayFormScreen(
    dayId: Long,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { AppRepository.get(context) }
    val settingsRepo = remember { SettingsRepository(context) }
    val settings by settingsRepo.settings.collectAsState(initial = AppSettings.DEFAULT)

    var loaded by remember { mutableStateOf(false) }
    var currentDayId by remember { mutableStateOf(if (dayId > 0L) dayId else 0L) }
    var clockInMs by remember { mutableStateOf<Long?>(null) }
    var clockOutMs by remember { mutableStateOf<Long?>(null) }
    var noteText by remember { mutableStateOf("") }
    var effectiveRate by remember { mutableStateOf(settings.hourlyRate) }
    val breaks = remember { mutableStateListOf<BreakDraft>() }

    var editingClockIn by remember { mutableStateOf(false) }
    var editingClockOut by remember { mutableStateOf(false) }
    var editingBreakIndex by remember { mutableStateOf<Int?>(null) }
    var editingBreakField by remember { mutableStateOf(0) }

    LaunchedEffect(dayId) {
        if (loaded) return@LaunchedEffect
        val now = System.currentTimeMillis()
        if (dayId > 0L) {
            val day = repo.getWorkDay(dayId)
            if (day != null) {
                currentDayId = day.id
                clockInMs = day.clockIn
                clockOutMs = day.clockOut ?: now
                noteText = day.note.orEmpty()
                effectiveRate = day.hourlyRate
                breaks.clear()
                repo.getBreaksForDay(day.id).forEach { b ->
                    breaks.add(BreakDraft(id = b.id, startMs = b.startTime, endMs = b.endTime))
                }
            }
        } else {
            clockInMs = now
            clockOutMs = null
            effectiveRate = settings.hourlyRate
        }
        loaded = true
    }

    val cin = clockInMs ?: 0L
    val cout = clockOutMs
    val totalMs = if (cout != null && cin > 0L) (cout - cin).coerceAtLeast(0L) else 0L
    val breakMs = breaks.sumOf { (it.endMs - it.startMs).coerceAtLeast(0L) }
    val actualMs = (totalMs - breakMs).coerceAtLeast(0L)
    val actualMin = actualMs / 60_000L
    val wage = (actualMin.toDouble() / 60.0) * effectiveRate

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (currentDayId == 0L) "إضافة يوم عمل" else "تعديل يوم عمل")
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
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
            // ---- Times ----
            AlamalCard {
                DateTimeRow(
                    label = "وقت الدخول",
                    ms = clockInMs,
                    onClick = { editingClockIn = true },
                )
                AlamalDivider()
                DateTimeRow(
                    label = "وقت الخروج",
                    ms = clockOutMs,
                    onClick = { editingClockOut = true },
                )
            }

            // ---- Breaks ----
            AlamalCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "الاستراحات",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (breaks.isNotEmpty()) {
                        Text(
                            text = "${breaks.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = LocalExtendedColors.current.muted,
                        )
                    }
                }

                if (breaks.isEmpty()) {
                    Text(
                        text = "لا توجد استراحات. أضف واحدة إن احتجت.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalExtendedColors.current.muted,
                    )
                } else {
                    breaks.forEachIndexed { index, b ->
                        BreakRow(
                            breakItem = b,
                            onEditStart = {
                                editingBreakIndex = index
                                editingBreakField = 0
                            },
                            onEditEnd = {
                                editingBreakIndex = index
                                editingBreakField = 1
                            },
                            onDelete = { breaks.removeAt(index) },
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        val start = System.currentTimeMillis()
                        breaks.add(BreakDraft(startMs = start, endMs = start + 30 * 60_000L))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("إضافة استراحة")
                }
            }

            // ---- Note ----
            AlamalCard {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("ملاحظة (اختياري)") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ---- Live stats ----
            AlamalCard {
                Text(
                    text = "النتيجة",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                AlamalDivider()
                AlamalStatRow(
                    "المدة الفعلية",
                    ArabicPlural.hoursMinutes(actualMin),
                    bold = true,
                )
                AlamalStatRow(
                    "الأجرة",
                    MoneyFormat.format(wage, settings.currency),
                    valueColor = LocalExtendedColors.current.accent,
                    bold = true,
                )
            }

            // ---- Save / Cancel ----
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
                        val startMs = clockInMs ?: return@Button
                        scope.launch {
                            val entity = WorkDayEntity(
                                id = currentDayId,
                                periodId = null,
                                clockIn = startMs,
                                clockOut = clockOutMs,
                                hourlyRate = effectiveRate,
                                note = noteText.ifBlank { null },
                            )
                            val breakEntities = breaks.map { d ->
                                BreakEntity(
                                    id = d.id,
                                    workDayId = 0L,
                                    startTime = d.startMs,
                                    endTime = d.endMs,
                                )
                            }
                            repo.upsertWorkDay(entity, breakEntities)
                            onDone()
                        }
                    },
                    enabled = clockInMs != null &&
                        (clockOutMs == null || (clockOutMs ?: 0L) > (clockInMs ?: 0L)),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text(stringResource(R.string.save)) }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (editingClockIn) {
        DateTimeWheelDialog(
            initialMs = clockInMs ?: System.currentTimeMillis(),
            title = "وقت الدخول",
            onDismiss = { editingClockIn = false },
            onConfirm = {
                clockInMs = it
                editingClockIn = false
            },
        )
    }
    if (editingClockOut) {
        DateTimeWheelDialog(
            initialMs = clockOutMs ?: System.currentTimeMillis(),
            title = "وقت الخروج",
            onDismiss = { editingClockOut = false },
            onConfirm = {
                clockOutMs = it
                editingClockOut = false
            },
        )
    }
    editingBreakIndex?.let { idx ->
        val b = breaks.getOrNull(idx)
        if (b != null) {
            DateTimeWheelDialog(
                initialMs = if (editingBreakField == 0) b.startMs else b.endMs,
                title = if (editingBreakField == 0) "بداية الاستراحة" else "نهاية الاستراحة",
                onDismiss = { editingBreakIndex = null },
                onConfirm = { picked ->
                    if (editingBreakField == 0) {
                        breaks[idx] = b.copy(startMs = picked)
                    } else {
                        breaks[idx] = b.copy(endMs = picked)
                    }
                    editingBreakIndex = null
                },
            )
        }
    }
}

@Composable
private fun DateTimeRow(
    label: String,
    ms: Long?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = LocalExtendedColors.current.muted,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (ms == null) "غير محدد" else DateFormatters.dateTime(ms),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        TextButton(onClick = onClick) {
            Text(if (ms == null) "تعيين" else "تغيير")
        }
    }
}

@Composable
private fun BreakRow(
    breakItem: BreakDraft,
    onEditStart: () -> Unit,
    onEditEnd: () -> Unit,
    onDelete: () -> Unit,
) {
    val minutes = ((breakItem.endMs - breakItem.startMs).coerceAtLeast(0L)) / 60_000L
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = ArabicPlural.hoursMinutes(minutes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${DateFormatters.time(breakItem.startMs)} → ${DateFormatters.time(breakItem.endMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalExtendedColors.current.muted,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onEditStart,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("من ${DateFormatters.time(breakItem.startMs)}")
            }
            OutlinedButton(
                onClick = onEditEnd,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("إلى ${DateFormatters.time(breakItem.endMs)}")
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}
