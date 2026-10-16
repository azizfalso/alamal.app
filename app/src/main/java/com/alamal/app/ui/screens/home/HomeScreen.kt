package com.alamal.app.ui.screens.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alamal.app.data.db.entity.WorkDayEntity
import com.alamal.app.ui.components.AlamalCard
import com.alamal.app.ui.components.AlamalDivider
import com.alamal.app.ui.components.SummaryShareDialog
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
            // ---- Header with secondary actions ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "الفترة الحالية",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HeaderIconButton(
                        icon = Icons.Default.Share,
                        contentDescription = "مشاركة الملخص",
                        onClick = { showShareDialog = true },
                    )
                    HeaderIconButton(
                        icon = Icons.Default.Insights,
                        contentDescription = "الإحصائيات",
                        onClick = onOpenStats,
                    )
                }
            }

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

            OutlinedButton(
                onClick = onOpenPayment,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "استلمت الدفعة",
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            if (state.totalDaysCount > 0) {
                PeriodSummaryCard(
                    daysCount = state.totalDaysCount,
                    totalMinutes = state.totalMinutes,
                    avgDailyMinutes = state.avgDailyMinutes,
                    avgDailyWage = state.avgDailyWage,
                    currency = state.currency,
                )
            }

            if (state.recentItems.isNotEmpty()) {
                RecentRecordsCard(
                    items = state.recentItems,
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
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
) {
    val border = LocalExtendedColors.current.border
    val accent = LocalExtendedColors.current.accent
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = accent,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun PeriodSummaryCard(
    daysCount: Int,
    totalMinutes: Long,
    avgDailyMinutes: Long,
    avgDailyWage: Double,
    currency: String,
    modifier: Modifier = Modifier,
) {
    // Only show the averages row when it adds information.
    // With a single day, average == total, which is redundant.
    val showAverages = daysCount > 1

    AlamalCard(modifier = modifier) {
        Text(
            text = "ملخص الفترة",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        AlamalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatTile(
                value = ArabicPlural.days(daysCount.toLong()),
                label = "أيام العمل",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = ArabicPlural.hoursMinutes(totalMinutes),
                label = "الساعات",
                modifier = Modifier.weight(1f),
            )
        }

        if (showAverages) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatTile(
                    value = MoneyFormat.format(avgDailyWage, currency),
                    label = "متوسط اليوم",
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = ArabicPlural.hoursMinutes(avgDailyMinutes),
                    label = "متوسط الساعات",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = LocalExtendedColors.current.muted,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RecentRecordsCard(
    items: List<RecentItem>,
    currency: String,
    modifier: Modifier = Modifier,
) {
    AlamalCard(modifier = modifier) {
        Text(
            text = "آخر التسجيلات",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        AlamalDivider()
        items.forEachIndexed { index, item ->
            RecentRow(item = item, currency = currency)
            if (index < items.lastIndex) {
                AlamalDivider()
            }
        }
    }
}

@Composable
private fun RecentRow(
    item: RecentItem,
    currency: String,
) {
    val accent = LocalExtendedColors.current.accent
    val muted = LocalExtendedColors.current.muted
    val onSurface = MaterialTheme.colorScheme.onSurface
    val errorColor = MaterialTheme.colorScheme.error

    val icon = when (item.kind) {
        RecentKind.WORK -> Icons.Default.Schedule
        RecentKind.REMOTE -> Icons.Default.Laptop
        RecentKind.CREDIT -> Icons.Default.MoneyOff
    }
    val title = when (item.kind) {
        RecentKind.WORK -> "يوم عمل"
        RecentKind.REMOTE -> "عن بُعد"
        RecentKind.CREDIT -> "كريدي"
    }
    val iconTint = when (item.kind) {
        RecentKind.CREDIT -> errorColor
        else -> accent
    }
    val iconBg = when (item.kind) {
        RecentKind.CREDIT -> errorColor.copy(alpha = 0.12f)
        else -> accent.copy(alpha = 0.12f)
    }
    val amountText = when (item.kind) {
        RecentKind.CREDIT -> "- " + MoneyFormat.format(item.amount, currency)
        else -> MoneyFormat.format(item.amount, currency)
    }
    val amountColor = when (item.kind) {
        RecentKind.CREDIT -> errorColor
        else -> onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                text = DateFormatters.dateArabicShort(item.dateMs),
                style = MaterialTheme.typography.bodySmall,
                color = muted,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = amountText,
            style = MaterialTheme.typography.bodyMedium,
            color = amountColor,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
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
    val onSurface = MaterialTheme.colorScheme.onSurface
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
            color = onSurface,
        )
    }
}

/**
 * Main balance card — centered, big bold number.
 *
 * The label "المؤجل" was renamed to "المرحَّل". Reason: "مؤجل" suggests
 * "postponed / to be paid later", which is misleading. The value is the
 * carried balance from the previous period (positive = owed to you,
 * negative = you owe). "المرحَّل" matches the long form already used in
 * the payment and archive screens ("الرصيد المرحَّل").
 */
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
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "المجموع الكامل",
            style = MaterialTheme.typography.labelLarge,
            color = muted,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatted,
            style = MaterialTheme.typography.displayMedium,
            color = numberColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )

        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(border),
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SplitCell(
                label = "المرحَّل",
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
    val surface = LocalExtendedColors.current.accentSurface
    val borderColor = LocalExtendedColors.current.accentSurfaceBorder
    val accent = LocalExtendedColors.current.accent
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surface)
            .border(1.dp, borderColor, shape)
            .clickable { onClockIn() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "تسجيل دخول العمل",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "اضغط لتسجيل وقت العمل الآن",
                style = MaterialTheme.typography.bodySmall,
                color = LocalExtendedColors.current.muted,
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

    val surface = LocalExtendedColors.current.workingSurface
    val borderColor = LocalExtendedColors.current.workingBorder
    val bigText = LocalExtendedColors.current.workingText
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = LocalExtendedColors.current.muted
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surface)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(borderColor),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "أنت الآن في العمل",
                style = MaterialTheme.typography.titleMedium,
                color = onSurface,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = "بدأت في ${DateFormatters.time(day.clockIn)}",
            style = MaterialTheme.typography.bodySmall,
            color = muted,
        )
        Text(
            text = "منذ ${ArabicPlural.hoursMinutes(elapsedMin)}",
            style = MaterialTheme.typography.headlineSmall,
            color = bigText,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onQuickStop,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = "إنهاء العمل الآن",
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onEditDetails,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
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
                    .height(40.dp),
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
