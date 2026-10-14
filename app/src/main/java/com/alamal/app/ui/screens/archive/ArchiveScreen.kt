package com.alamal.app.ui.screens.archive

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alamal.app.data.RollbackResult
import com.alamal.app.data.db.entity.PeriodEntity
import com.alamal.app.data.prefs.AppSettings
import com.alamal.app.data.prefs.SettingsRepository
import com.alamal.app.domain.MoneyCalculator
import com.alamal.app.domain.SummaryDay
import com.alamal.app.domain.SummaryInput
import com.alamal.app.domain.SummaryShareBuilder
import com.alamal.app.ui.components.AlamalCard
import com.alamal.app.ui.components.AlamalDivider
import com.alamal.app.ui.components.AlamalStatRow
import com.alamal.app.ui.components.SummaryShareDialog
import com.alamal.app.ui.theme.LocalExtendedColors
import com.alamal.app.util.ArabicPlural
import com.alamal.app.util.DateFormatters
import com.alamal.app.util.MoneyFormat

@Composable
fun ArchiveScreen() {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }
    val settings by settingsRepo.settings.collectAsState(initial = AppSettings.DEFAULT)
    val currency = settings.currency

    val vm: ArchiveViewModel = viewModel(factory = ArchiveViewModel.factory(context))
    val state by vm.state.collectAsState()

    var pendingRollback by remember { mutableStateOf<PeriodEntity?>(null) }
    var sharePeriod by remember { mutableStateOf<PeriodEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
            state.periods.isEmpty() -> EmptyState()
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
                ) {
                    item {
                        Text(
                            text = "الأرشيف",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    items(state.periods, key = { it.id }) { period ->
                        val isLatest = period.id == state.periods.first().id
                        PeriodCard(
                            period = period,
                            isLatest = isLatest,
                            isExpanded = state.expandedPeriodId == period.id,
                            details = if (state.expandedPeriodId == period.id) state.details else null,
                            loadingDetails = state.loadingDetails && state.expandedPeriodId == period.id,
                            currency = currency,
                            onToggle = { vm.toggleExpand(period.id) },
                            onShare = { sharePeriod = period },
                            onRequestRollback = { pendingRollback = period },
                        )
                    }
                }
            }
        }
    }

    pendingRollback?.let { period ->
        AlertDialog(
            onDismissRequest = { pendingRollback = null },
            title = { Text("تراجع عن آخر دفعة") },
            text = {
                Text(
                    "سيتم حذف دفعة ${DateFormatters.dateArabicShort(period.endDate)} من الأرشيف، " +
                        "وإعادة كل سجلاتها إلى الفترة الحالية، وإلغاء الرصيد المرحَّل الناتج عنها."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRollback = null
                        vm.rollbackLastPayment { result ->
                            val msg = when (result) {
                                is RollbackResult.Success ->
                                    "تم التراجع عن الدفعة. أُعيد ${result.restoredRecords} سجلًا."
                                is RollbackResult.Blocked ->
                                    "لا يمكن التراجع: هناك ${result.newRecordsCount} سجل جديد في الفترة الحالية. احذفها أولًا."
                                RollbackResult.NoPayments ->
                                    "لا توجد دفعات للتراجع عنها."
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                ) { Text("تراجع") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRollback = null }) { Text("إلغاء") }
            },
        )
    }

    sharePeriod?.let { period ->
        SummaryShareDialog(
            onDismiss = { sharePeriod = null },
            onShare = { includeDetails ->
                val details = state.details
                sharePeriod = null
                val text = if (details != null && state.expandedPeriodId == period.id) {
                    val input = details.toSummaryInput()
                    SummaryShareBuilder.build(input, currency, includeDetails)
                } else {
                    buildBasicSummary(period, currency)
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, "مشاركة الملخص"))
            },
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "لا يوجد أرشيف بعد",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "ستظهر هنا كل دفعة استلمتها مع تفاصيل الفترة.",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalExtendedColors.current.muted,
        )
    }
}

@Composable
private fun PeriodCard(
    period: PeriodEntity,
    isLatest: Boolean,
    isExpanded: Boolean,
    details: PeriodDetails?,
    loadingDetails: Boolean,
    currency: String,
    onToggle: () -> Unit,
    onShare: () -> Unit,
    onRequestRollback: () -> Unit,
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(220),
        label = "arrowRotation",
    )

    AlamalCard(contentPadding = 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${DateFormatters.dateArabicShort(period.startDate)} ← ${DateFormatters.dateArabicShort(period.endDate)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "مستحق: ${MoneyFormat.formatSigned(period.amountDue, currency)} • مستلم: ${MoneyFormat.format(period.amountReceived, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalExtendedColors.current.muted,
                )
            }
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = LocalExtendedColors.current.accent,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { rotationZ = arrowRotation },
            )
        }

        AlamalDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AlamalStatRow(
                "الرصيد المرحَّل",
                MoneyFormat.formatSigned(period.carriedBalance, currency),
                valueColor = colorOf(period.carriedBalance),
            )
            AlamalStatRow("أجور الساعات", MoneyFormat.format(period.totalWages, currency))
            if (period.totalRemote > 0.0001) {
                AlamalStatRow("منها عن بُعد", MoneyFormat.format(period.totalRemote, currency))
            }
            if (period.totalCredits > 0.0001) {
                AlamalStatRow(
                    "الكريدي",
                    "- " + MoneyFormat.format(period.totalCredits, currency),
                    valueColor = MaterialTheme.colorScheme.error,
                )
            }
            AlamalDivider()
            AlamalStatRow(
                "المستحق",
                MoneyFormat.formatSigned(period.amountDue, currency),
                valueColor = colorOf(period.amountDue),
                bold = true,
            )
            AlamalStatRow("المستلم", MoneyFormat.format(period.amountReceived, currency))

            val remaining = period.remaining
            AlamalStatRow(
                label = when {
                    remaining > 0.0001 -> "الباقي"
                    remaining < -0.0001 -> "الزائد"
                    else -> "الحساب"
                },
                value = MoneyFormat.formatSigned(remaining, currency),
                valueColor = colorOf(remaining),
                bold = true,
            )

            if (!period.note.isNullOrBlank()) {
                Text(
                    text = "ملاحظة: ${period.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalExtendedColors.current.meta,
                )
            }
        }

        if (isExpanded) {
            AlamalDivider()
            if (loadingDetails) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            } else if (details != null) {
                PeriodDetailsSection(details = details, currency = currency)
            }
        }

        AlamalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onShare,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("مشاركة")
            }
            if (isLatest) {
                OutlinedButton(
                    onClick = onRequestRollback,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تراجع")
                }
            }
        }
    }
}

@Composable
private fun PeriodDetailsSection(details: PeriodDetails, currency: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "التفاصيل (${ArabicPlural.days(details.workDays.size.toLong())} • ${ArabicPlural.hoursMinutes(details.totalMinutes)})",
            style = MaterialTheme.typography.titleSmall,
            color = LocalExtendedColors.current.accent,
            fontWeight = FontWeight.Medium,
        )

        details.workDays.forEachIndexed { idx, day ->
            val brk = details.breaksByDay[day.id].orEmpty()
            val stats = MoneyCalculator.computeDayStats(day, brk)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${idx + 1}. ${DateFormatters.dateArabicShort(day.clockIn)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = MoneyFormat.format(stats.wage, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "${DateFormatters.time(day.clockIn)} → ${day.clockOut?.let { DateFormatters.time(it) } ?: "—"}" +
                        " • ${ArabicPlural.hoursMinutes(stats.actualMinutes)}" +
                        if (brk.isNotEmpty()) " • ${brk.size} استراحة" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalExtendedColors.current.muted,
                )
                if (!day.note.isNullOrBlank()) {
                    Text(
                        text = day.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalExtendedColors.current.meta,
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        if (details.remoteWorks.isNotEmpty()) {
            Text(
                text = "العمل عن بُعد",
                style = MaterialTheme.typography.titleSmall,
                color = LocalExtendedColors.current.accent,
                fontWeight = FontWeight.Medium,
            )
            details.remoteWorks.forEach { rw ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = DateFormatters.dateArabicShort(rw.date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = MoneyFormat.format(rw.amount, currency),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (!rw.note.isNullOrBlank()) {
                        Text(
                            text = rw.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalExtendedColors.current.meta,
                        )
                    }
                }
            }
        }

        if (details.credits.isNotEmpty()) {
            Text(
                text = "الكريدي",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium,
            )
            details.credits.forEach { c ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = DateFormatters.dateArabicShort(c.date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "- " + MoneyFormat.format(c.amount, currency),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (!c.note.isNullOrBlank()) {
                        Text(
                            text = c.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalExtendedColors.current.meta,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun colorOf(value: Double): Color = when {
    value < -0.0001 -> MaterialTheme.colorScheme.error
    value > 0.0001 -> LocalExtendedColors.current.success
    else -> MaterialTheme.colorScheme.onSurface
}

private fun buildBasicSummary(period: PeriodEntity, currency: String): String {
    val sb = StringBuilder()
    sb.appendLine("ملخص الدفعة")
    sb.appendLine(
        "الفترة: ${DateFormatters.dateArabicShort(period.startDate)} ← ${DateFormatters.dateArabicShort(period.endDate)}"
    )
    sb.appendLine()
    if (kotlin.math.abs(period.carriedBalance) > 0.0001) {
        sb.appendLine("الرصيد المرحَّل: ${MoneyFormat.formatSigned(period.carriedBalance, currency)}")
    }
    sb.appendLine("الأجور: ${MoneyFormat.format(period.totalWages, currency)}")
    if (period.totalRemote > 0.0001) {
        sb.appendLine("منها عن بُعد: ${MoneyFormat.format(period.totalRemote, currency)}")
    }
    if (period.totalCredits > 0.0001) {
        sb.appendLine("الكريدي: - ${MoneyFormat.format(period.totalCredits, currency)}")
    }
    sb.appendLine("المستحق: ${MoneyFormat.formatSigned(period.amountDue, currency)}")
    sb.appendLine("المستلم: ${MoneyFormat.format(period.amountReceived, currency)}")
    val remaining = period.remaining
    val label = when {
        remaining > 0.0001 -> "الباقي"
        remaining < -0.0001 -> "الزائد"
        else -> "الحساب"
    }
    sb.appendLine("$label: ${MoneyFormat.formatSigned(remaining, currency)}")
    if (!period.note.isNullOrBlank()) {
        sb.appendLine()
        sb.appendLine("ملاحظة: ${period.note}")
    }
    return sb.toString().trimEnd()
}

private fun PeriodDetails.toSummaryInput(): SummaryInput {
    val days = workDays.map { day ->
        val brk = breaksByDay[day.id].orEmpty()
        val stats = MoneyCalculator.computeDayStats(day, brk)
        SummaryDay(
            clockIn = day.clockIn,
            clockOut = day.clockOut,
            breaks = brk,
            actualMinutes = stats.actualMinutes,
            wage = stats.wage,
            note = day.note,
        )
    }
    return SummaryInput(
        startDate = period.startDate,
        endDate = period.endDate,
        carriedBalance = period.carriedBalance,
        totalWages = period.totalWages,
        totalRemote = period.totalRemote,
        totalCredits = period.totalCredits,
        amountDue = period.amountDue,
        totalMinutes = totalMinutes,
        days = days,
        remotes = remoteWorks,
        credits = credits,
    )
}
