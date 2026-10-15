package com.alamal.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alamal.app.util.DateFormatters
import java.util.Calendar

/**
 * A date/time picker dialog based on three+two wheel columns.
 * Layout (RTL):
 *   [سنة] [شهر] [يوم]    -- year on the left, day on the right
 *   [ساعة] [دقيقة]       -- hour on the right, minute on the left
 */
@Composable
fun DateTimeWheelDialog(
    initialMs: Long,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val cal = remember(initialMs) {
        Calendar.getInstance().apply { timeInMillis = initialMs }
    }
    val baseYear = cal.get(Calendar.YEAR)

    var year by remember { mutableIntStateOf(baseYear) }
    var month by remember { mutableIntStateOf(cal.get(Calendar.MONTH)) } // 0..11
    var day by remember { mutableIntStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
    var hour by remember { mutableIntStateOf(cal.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(cal.get(Calendar.MINUTE)) }

    // Year range widened from (baseYear - 5 .. baseYear + 1) to
    // (baseYear - 10 .. baseYear + 1) so users can back-date shifts that
    // happened further in the past without being blocked by the picker.
    val yearList = remember(baseYear) { (baseYear - 10..baseYear + 1).toList() }
    val monthList = remember { (1..12).toList() }
    val daysInMonth = remember(year, month) { computeDaysInMonth(year, month) }
    val dayList = remember(daysInMonth) { (1..daysInMonth).toList() }
    val hourList = remember { (0..23).toList() }
    val minuteList = remember { (0..59).toList() }

    LaunchedEffect(year, month) {
        if (day > daysInMonth) day = daysInMonth
    }

    val previewMs = remember(year, month, day, hour, minute) {
        makeMs(year, month, day, hour, minute)
    }

    fun applyNowOffset(offsetMs: Long) {
        val c = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis() + offsetMs
        }
        year = c.get(Calendar.YEAR)
        month = c.get(Calendar.MONTH)
        day = c.get(Calendar.DAY_OF_MONTH)
        hour = c.get(Calendar.HOUR_OF_DAY)
        minute = c.get(Calendar.MINUTE)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = DateFormatters.dateTime(previewMs),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AssistChip(
                        onClick = { applyNowOffset(0L) },
                        label = { Text("الآن") },
                    )
                    AssistChip(
                        onClick = { applyNowOffset(-60L * 60_000L) },
                        label = { Text("قبل ساعة") },
                    )
                    AssistChip(
                        onClick = { applyNowOffset(-24L * 60L * 60_000L) },
                        label = { Text("أمس") },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WheelColumn(
                        label = "يوم",
                        items = dayList.map { it.toString().padStart(2, '0') },
                        selectedIndex = dayList.indexOf(day).coerceAtLeast(0),
                        onSelectedIndexChange = { idx ->
                            day = dayList.getOrElse(idx) { day }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    WheelColumn(
                        label = "شهر",
                        items = monthList.map { monthArabicName(it) },
                        selectedIndex = month.coerceIn(0, 11),
                        onSelectedIndexChange = { idx ->
                            month = idx.coerceIn(0, 11)
                        },
                        modifier = Modifier.weight(1.6f),
                    )
                    WheelColumn(
                        label = "سنة",
                        items = yearList.map { it.toString() },
                        selectedIndex = yearList.indexOf(year).coerceAtLeast(0),
                        onSelectedIndexChange = { idx ->
                            year = yearList.getOrElse(idx) { year }
                        },
                        modifier = Modifier.weight(1.1f),
                    )
                }

                Spacer(Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WheelColumn(
                        label = "ساعة",
                        items = hourList.map { it.toString().padStart(2, '0') },
                        selectedIndex = hour.coerceIn(0, 23),
                        onSelectedIndexChange = { idx ->
                            hour = hourList.getOrElse(idx) { hour }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    WheelColumn(
                        label = "دقيقة",
                        items = minuteList.map { it.toString().padStart(2, '0') },
                        selectedIndex = minute.coerceIn(0, 59),
                        onSelectedIndexChange = { idx ->
                            minute = minuteList.getOrElse(idx) { minute }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(previewMs) }) { Text("موافق") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}

@Composable
private fun WheelColumn(
    label: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        WheelPicker(
            items = items,
            selectedIndex = selectedIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0)),
            onSelectedIndexChange = onSelectedIndexChange,
            modifier = Modifier.fillMaxWidth(),
            itemHeight = 36.dp,
            visibleItems = 3,
            textStyle = MaterialTheme.typography.titleMedium,
        )
    }
}

private fun monthArabicName(oneBased: Int): String = when (oneBased) {
    1 -> "يناير"
    2 -> "فبراير"
    3 -> "مارس"
    4 -> "أبريل"
    5 -> "مايو"
    6 -> "يونيو"
    7 -> "يوليو"
    8 -> "أغسطس"
    9 -> "سبتمبر"
    10 -> "أكتوبر"
    11 -> "نوفمبر"
    else -> "ديسمبر"
}

private fun computeDaysInMonth(year: Int, monthZeroBased: Int): Int {
    val c = Calendar.getInstance()
    c.set(Calendar.YEAR, year)
    c.set(Calendar.MONTH, monthZeroBased)
    c.set(Calendar.DAY_OF_MONTH, 1)
    return c.getActualMaximum(Calendar.DAY_OF_MONTH)
}

private fun makeMs(y: Int, m0: Int, d: Int, h: Int, mi: Int): Long {
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, y)
        set(Calendar.MONTH, m0)
        set(Calendar.DAY_OF_MONTH, d)
        set(Calendar.HOUR_OF_DAY, h)
        set(Calendar.MINUTE, mi)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
