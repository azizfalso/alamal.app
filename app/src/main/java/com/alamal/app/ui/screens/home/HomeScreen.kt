package com.alamal.app.ui.screens.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alamal.app.data.db.entity.WorkDayEntity
import com.alamal.app.ui.components.AlamalCard
import com.alamal.app.ui.components.AlamalDivider
import com.alamal.app.ui.components.AlamalStatRow
import com.alamal.app.ui.components.SummaryShareDialog
import com.alamal.app.ui.components.dashedBorder
import com.alamal.app.ui.theme.LocalExtendedColors
import com.alamal.app.util.ArabicPlural
import com.alamal.app.util.DateFormatters
import com.alamal.app.util.MoneyFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onOpenWorkDayForm: (Long) -> Unit,
    onOpenRemoteWorkForm: () -> Unit,
    onOpenCreditForm: () -> Unit,
    onOpenPayment: () -> Unit,
    onOpenStats: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(context))
    val state by vm.state.collectAsState()
    var showShareDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "الفترة الحالية",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 2.dp),
            )

            UnifiedSummaryCard(
                net = state.netBalance,
                carried = state.carriedBalance,
                credits = state.totalCredits,
                wages = state.totalWages,
                currency = state.currency,
            )

            val open = state.openWorkDay
            if (open != null) {
                AtWorkCard(
                    day = open,
                    onQuickStop = { vm.clockOutNow() },
                    onEditDetails = { onOpenWorkDayForm(open.id) },
                    onCancelClockIn = { vm.cancelClockIn() },
                )
            } else {
                ClockInCard(onClockIn = { vm.clockIn() })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompactActionButton(
                    icon = Icons.Default.Share,
                    label = "مشاركة الملخص",
                    onClick = { showShareDialog = true },
                    modifier = Modifier.weight(1f),
                )
                CompactActionButton(
                    icon = Icons.Default.Insights,
                    label = "الإحصائيات",
                    onClick = onOpenStats,
                    modifier = Modifier.weight(1f),
                )
            }

            Button(
                onClick = onOpenPayment,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "استلمت الدفعة",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            // ---- Quick stats card ----
            // Fills the previously empty area below the payment button.
            // Only appears once at least one work day has been recorded.
            if (state.totalDaysCount > 0) {
                QuickStatsCard(
                    daysCount = state.totalDaysCount,
                    completedDays = state.workDaysCount,
                    totalMinutes = state.totalMinutes,
                    totalWages = state.totalWages,
                    currency = state.currency,
                )
            }

            Spacer(Modifier.height(4.dp))
        }

        HomeActionBar(
            onWorkDay = { onOpenWorkDayForm(-1L) },
            onRemote = onOpenRemoteWorkForm,
            onCredit = onOpenCreditForm,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }

    if (showShareDialog) {
        SummaryShareDialog(
            onDismiss = { showShareDialog = false },
            onShare = { includeDetails ->
                showShareDialog = false
                scope.launch {
                    val text = vm.buildCurrentSummaryText(includeDetails)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, "مشاركة الملخص"))
                }
            },
        )
    }
}

@Composable
private fun QuickStatsCard(
    daysCount: Int,
    completedDays: Int,
    totalMinutes: Long,
    totalWages: Double,
    currency: String,
    modifier: Modifier = Modifier,
) {
    val avgPerDay = if (completedDays > 0) totalWages / completedDays else 0.0
    val avgPerHour = if (totalMinutes > 0) totalWages / (totalMinutes / 60.0) else 0.0

    AlamalCard(modifier = modifier) {
        Text(
            text = "ملخص سريع",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        AlamalDivider()
        AlamalStatRow(
            label = "أيام مسجّلة",
            value = ArabicPlural.days(daysCount.toLong()),
        )
        AlamalStatRow(
            label = "الساعات",
            value = ArabicPlural.hoursMinutes(totalMinutes),
        )
        if (avgPerDay > 0.0001) {
            AlamalStatRow(
                label = "متوسط اليوم",
                value = MoneyFormat.format(avgPerDay, currency),
            )
        }
        if (avgPerHour > 0.0001) {
            AlamalStatRow(
                label = "متوسط الساعة",
                value = MoneyFormat.format(avgPerHour, currency),
            )
        }
    }
}

@Composable
private fun HomeActionBar(
    onWorkDay: () -> Unit,
    onRemote: () -> Unit,
    onCredit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val bg = MaterialTheme.colorScheme.surfaceContainerLow
    val border = LocalExtendedColors.current.border

    Row(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeActionItem(
            icon = Icons.Default.Schedule,
            label = "يوم عمل",
            onClick = onWorkDay,
            tint = LocalExtendedColors.current.accent,
            modifier = Modifier.weight(1f),
        )
        HomeActionItem(
            icon = Icons.Default.Laptop,
            label = "عن بُعد",
            onClick = onRemote,
            tint = LocalExtendedColors.current.accent,
            modifier = Modifier.weight(1f),
        )
        HomeActionItem(
            icon = Icons.Default.MoneyOff,
            label = "كريدي",
            onClick = onCredit,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HomeActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun UnifiedSummaryCard(
    net: Double,
    carried: Double,
    credits: Double,
    wages: Double,
    currency: String,
) {
    val accent = LocalExtendedColors.current.accent
    val muted = LocalExtendedColors.current.muted
    val border = LocalExtendedColors.current.border
    val onSurface = MaterialTheme.colorScheme.onSurface
    val isNegative = net < -0.0001

    val bg = if (isNegative)
        MaterialTheme.colorScheme.errorContainer
    else
        MaterialTheme.colorScheme.surfaceContainerLow

    val numberColor = if (isNegative)
        MaterialTheme.colorScheme.error
    else
        accent

    val borderColor = if (isNegative)
        MaterialTheme.colorScheme.error.copy(alpha = 0.30f)
    else
        border

    val formatted = MoneyFormat.formatSigned(net, currency).replace(" ", "\u00A0")
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "المجموع الكامل",
            style = MaterialTheme.typography.labelLarge,
            color = muted,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatted,
            style = MaterialTheme.typography.displaySmall,
            color = numberColor,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(border),
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SplitCell(
                label = "مؤجل",
                value = MoneyFormat.formatSigned(carried, currency),
                labelColor = muted,
                valueColor = onSurface,
            )
            VerticalDividerLine(color = border)
            SplitCell(
                label = "الكريدي",
                value = if (credits > 0.0001)
                    "- " + MoneyFormat.format(credits, currency)
                else MoneyFormat.format(0.0, currency),
                labelColor = muted,
                valueColor = if (credits > 0.0001)
                    MaterialTheme.colorScheme.error
                else
                    onSurface,
            )
            VerticalDividerLine(color = border)
            SplitCell(
                label = "الأجور",
                value = MoneyFormat.format(wages, currency),
                labelColor = muted,
                valueColor = onSurface,
            )
        }
    }
}

@Composable
private fun SplitCell(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun VerticalDividerLine(color: Color) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(color),
    )
}

@Composable
private fun ClockInCard(onClockIn: () -> Unit) {
    val border = LocalExtendedColors.current.borderStrong
    val accent = LocalExtendedColors.current.accent
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .dashedBorder(color = border, cornerRadius = 16.dp)
            .clickable { onClockIn() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "تسجيل دخول العمل",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "اضغط لتسجيل وقت العمل الآن",
                style = MaterialTheme.typography.bodySmall,
                color = LocalExtendedColors.current.muted,
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun AtWorkCard(
    day: WorkDayEntity,
    onQuickStop: () -> Unit,
    onEditDetails: () -> Unit,
    onCancelClockIn: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            now = System.currentTimeMillis()
        }
    }
    val elapsedMin = ((now - day.clockIn).coerceAtLeast(0L)) / 60_000L
    val success = LocalExtendedColors.current.success

    AlamalCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(success),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "أنت الآن في العمل",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = "بدأت في ${DateFormatters.time(day.clockIn)}",
            style = MaterialTheme.typography.bodySmall,
            color = LocalExtendedColors.current.muted,
        )
        Text(
            text = "منذ ${ArabicPlural.hoursMinutes(elapsedMin)}",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(2.dp))
        Button(
            onClick = onQuickStop,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                text = "إنهاء العمل الآن",
                fontWeight = FontWeight.Medium,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onEditDetails,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = "تعديل التفاصيل",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            OutlinedButton(
                onClick = onCancelClockIn,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "إلغاء الدخول",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CompactActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = LocalExtendedColors.current.accent,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
