package com.alamal.app.ui.screens.workday

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.alamal.app.R
import com.alamal.app.data.AppRepository
import com.alamal.app.data.db.entity.BreakEntity
import com.alamal.app.data.db.entity.WorkDayEntity
import com.alamal.app.data.prefs.AppSettings
import com.alamal.app.data.prefs.SettingsRepository
import com.alamal.app.util.ArabicPlural
import com.alamal.app.util.DateFormatters
import com.alamal.app.util.MoneyFormat
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

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
    // سعر اليوم: يُثبَّت مرة واحدة عند التحميل ولا يتغيّر بالواجهة.
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
                title = { Text(if (currentDayId == 0L) "إضافة يوم عمل" else "تعديل يوم عمل") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DateTimeRow(
                label = "وقت الدخول",
                ms = clockInMs,
                onClick = { editingClockIn = true },
            )
            DateTimeRow(
                label = "وقت الخروج",
                ms = clockOutMs,
                onClick = { editingClockOut = true },
            )

            Text(
                text = "الاستراحات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            breaks.forEachIndexed { index, b ->
                BreakCard(
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

            OutlinedButton(
                onClick = {
                    val start = System.currentTimeMillis()
                    breaks.add(BreakDraft(startMs = start, endMs = start + 30 * 60_000L))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("إضافة استراحة")
            }

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("ملاحظة (اختياري)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            StatsCard(actualMin = actualMin, wage = wage, currency = settings.currency)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDone,
                    modifier = Modifier.weight(1f),
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
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.save)) }
            }
        }
    }

    if (editingClockIn) {
        DateTimePickerDialog(
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
        DateTimePickerDialog(
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
            DateTimePickerDialog(
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
private fun StatsCard(actualMin: Long, wage: Double, currency: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("المدة الفعلية", style = MaterialTheme.typography.labelMedium)
            Text(
                text = ArabicPlural.hoursMinutes(actualMin),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text("الأجرة", style = MaterialTheme.typography.labelMedium)
            Text(
                text = MoneyFormat.format(wage, currency),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (ms == null) "غير محدد" else DateFormatters.dateTime(ms),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
            TextButton(onClick = onClick) {
                Text(if (ms == null) "تعيين" else "تغيير")
            }
        }
    }
}

@Composable
private fun BreakCard(
    breakItem: BreakDraft,
    onEditStart: () -> Unit,
    onEditEnd: () -> Unit,
    onDelete: () -> Unit,
) {
    val minutes = ((breakItem.endMs - breakItem.startMs).coerceAtLeast(0L)) / 60_000L
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "استراحة • ${ArabicPlural.hoursMinutes(minutes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
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
                ) {
                    Text("من ${DateFormatters.time(breakItem.startMs)}")
                }
                OutlinedButton(
                    onClick = onEditEnd,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("إلى ${DateFormatters.time(breakItem.endMs)}")
                }
            }
        }
    }
}

// ============================================================
// منتقي التاريخ والوقت الجديد
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerDialog(
    initialMs: Long,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val cal = remember(initialMs) {
        Calendar.getInstance().apply { timeInMillis = initialMs }
    }
    var year by remember { mutableStateOf(cal.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(cal.get(Calendar.MONTH)) }
    var day by remember { mutableStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
    var hour by remember { mutableStateOf(cal.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableStateOf(cal.get(Calendar.MINUTE)) }

    var dateText by remember {
        mutableStateOf(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day))
    }
    var timeText by remember {
        mutableStateOf(String.format(Locale.US, "%02d:%02d", hour, minute))
    }
    var dateError by remember { mutableStateOf(false) }
    var timeError by remember { mutableStateOf(false) }

    var showNestedCalendar by remember { mutableStateOf(false) }
    var showNestedTimeInput by remember { mutableStateOf(false) }

    // اشتقاق القيمة النهائية من حقلي الإدخال
    val previewMs: Long? = remember(dateText, timeText) {
        parseDateTime(dateText, timeText)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // أزرار سريعة
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AssistChip(
                        onClick = {
                            val now = System.currentTimeMillis()
                            applyMs(now) { y, m, d, h, mi ->
                                year = y; month = m; day = d; hour = h; minute = mi
                            }
                            dateText = formatDateForInput(now)
                            timeText = formatTimeForInput(now)
                            dateError = false; timeError = false
                        },
                        label = { Text("الآن") },
                    )
                    AssistChip(
                        onClick = {
                            val now = System.currentTimeMillis() - 60 * 60_000L
                            applyMs(now) { y, m, d, h, mi ->
                                year = y; month = m; day = d; hour = h; minute = mi
                            }
                            dateText = formatDateForInput(now)
                            timeText = formatTimeForInput(now)
                            dateError = false; timeError = false
                        },
                        label = { Text("قبل ساعة") },
                    )
                    AssistChip(
                        onClick = {
                            val now = System.currentTimeMillis() - 24 * 60 * 60_000L
                            applyMs(now) { y, m, d, h, mi ->
                                year = y; month = m; day = d; hour = h; minute = mi
                            }
                            dateText = formatDateForInput(now)
                            timeText = formatTimeForInput(now)
                            dateError = false; timeError = false
                        },
                        label = { Text("أمس") },
                    )
                }

                // حقل التاريخ
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(8)
                        dateText = buildString {
                            for (i in digits.indices) {
                                if (i == 4 || i == 6) append('-')
                                append(digits[i])
                            }
                        }
                        dateError = false
                    },
                    label = { Text("التاريخ") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    isError = dateError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = { showNestedCalendar = true }) {
                                Text("تقويم", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    },
                )

                // حقل الوقت
                OutlinedTextField(
                    value = timeText,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(4)
                        timeText = buildString {
                            for (i in digits.indices) {
                                if (i == 2) append(':')
                                append(digits[i])
                            }
                        }
                        timeError = false
                    },
                    label = { Text("الوقت (24 ساعة)") },
                    placeholder = { Text("HH:MM") },
                    singleLine = true,
                    isError = timeError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = { showNestedTimeInput = true }) {
                                Text("ساعة", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    },
                )

                // معاينة
                val preview = previewMs
                if (preview != null) {
                    Text(
                        text = "سيكون: ${DateFormatters.dateTime(preview)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (dateText.length == 10 || timeText.length == 5) {
                    Text(
                        text = "صيغة غير صحيحة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = previewMs != null,
                onClick = {
                    val ms = previewMs
                    if (ms != null) onConfirm(ms) else {
                        // تمكين الخطأ المرئي
                        dateError = true
                        timeError = true
                    }
                },
            ) { Text("موافق") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )

    // ---- تقويم Material 3 المرئي (اختياري) ----
    if (showNestedCalendar) {
        val initialSelected = previewMs ?: initialMs
        val dateState = rememberDatePickerState(initialSelectedDateMillis = initialSelected)
        DatePickerDialog(
            onDismissRequest = { showNestedCalendar = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = dateState.selectedDateMillis
                        if (picked != null) {
                            val c = Calendar.getInstance().apply { timeInMillis = picked }
                            year = c.get(Calendar.YEAR)
                            month = c.get(Calendar.MONTH)
                            day = c.get(Calendar.DAY_OF_MONTH)
                            dateText = String.format(
                                Locale.US, "%04d-%02d-%02d",
                                year, month + 1, day,
                            )
                            dateError = false
                        }
                        showNestedCalendar = false
                    },
                ) { Text("موافق") }
            },
            dismissButton = {
                TextButton(onClick = { showNestedCalendar = false }) { Text("إلغاء") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    // ---- إدخال الوقت Material 3 (اختياري) ----
    if (showNestedTimeInput) {
        val tState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showNestedTimeInput = false },
            title = { Text("اختر الوقت") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TimeInput(state = tState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        hour = tState.hour
                        minute = tState.minute
                        timeText = String.format(Locale.US, "%02d:%02d", hour, minute)
                        timeError = false
                        showNestedTimeInput = false
                    },
                ) { Text("موافق") }
            },
            dismissButton = {
                TextButton(onClick = { showNestedTimeInput = false }) { Text("إلغاء") }
            },
        )
    }
}

// ---- دوال مساعدة ----

private fun applyMs(
    ms: Long,
    apply: (y: Int, m: Int, d: Int, h: Int, mi: Int) -> Unit,
) {
    val c = Calendar.getInstance().apply { timeInMillis = ms }
    apply(
        c.get(Calendar.YEAR),
        c.get(Calendar.MONTH),
        c.get(Calendar.DAY_OF_MONTH),
        c.get(Calendar.HOUR_OF_DAY),
        c.get(Calendar.MINUTE),
    )
}

private fun formatDateForInput(ms: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = ms }
    return String.format(
        Locale.US, "%04d-%02d-%02d",
        c.get(Calendar.YEAR),
        c.get(Calendar.MONTH) + 1,
        c.get(Calendar.DAY_OF_MONTH),
    )
}

private fun formatTimeForInput(ms: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = ms }
    return String.format(
        Locale.US, "%02d:%02d",
        c.get(Calendar.HOUR_OF_DAY),
        c.get(Calendar.MINUTE),
    )
}

/**
 * يحوّل نص التاريخ "YYYY-MM-DD" ونص الوقت "HH:MM" إلى مللي ثانية.
 * يعيد null إذا كانت الصيغة غير صحيحة أو التاريخ غير موجود (مثل 30 فبراير).
 */
private fun parseDateTime(dateText: String, timeText: String): Long? {
    return try {
        if (dateText.length != 10 || timeText.length != 5) return null
        val dp = dateText.split("-")
        val tp = timeText.split(":")
        if (dp.size != 3 || tp.size != 2) return null

        val y = dp[0].toInt()
        val m = dp[1].toInt() - 1
        val d = dp[2].toInt()
        val h = tp[0].toInt()
        val mi = tp[1].toInt()

        if (m < 0 || m > 11) return null
        if (d < 1 || d > 31) return null
        if (h < 0 || h > 23) return null
        if (mi < 0 || mi > 59) return null
        if (y < 1970 || y > 2200) return null

        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, y)
            set(Calendar.MONTH, m)
            set(Calendar.DAY_OF_MONTH, d)
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, mi)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // تحقق أن الأيام لم تُلتفّ (مثل 31 فبراير → 3 مارس)
        if (c.get(Calendar.YEAR) != y) return null
        if (c.get(Calendar.MONTH) != m) return null
        if (c.get(Calendar.DAY_OF_MONTH) != d) return null

        c.timeInMillis
    } catch (_: Exception) {
        null
    }
}
