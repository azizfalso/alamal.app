package com.alamal.app.ui.screens.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.style.TextOverflow
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
import kotlin.math.abs

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Column {
            Text(
                text = "الفترة الحالية",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "ملخّص أرباحك منذ آخر دفعة",
                style = MaterialTheme.typography.bodySmall,
                color = LocalExtendedColors.current.muted,
            )
        }

        Spacer(Modifier.height(2.dp))

        NetCard(net = state.netBalance, currency = state.currency)

        SummaryCard(
            totalMinutes = state.totalMinutes,
            totalWages = state.totalWages,
            totalRemote = state.totalRemote,
            totalCredits = state.totalCredits,
            carried = state.carriedBalance,
            currency = state.currency,
            workDaysCount = state.workDaysCount,
        )

        val open = state.openWorkDay
        if (open != null) {
            AtWorkCard(day = open, onClockOut = { onOpenWorkDayForm(open.id) })
        } else {
            ClockInCard(onClockIn = { vm.clockIn() })
        }

        // Quick actions row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilledTonalButton(
                onClick = onOpenRemoteWorkForm,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    Icons.Default.Laptop,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("عمل عن بُعد")
            }
            FilledTonalButton(
                onClick = onOpenCreditForm,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    Icons.Default.MoneyOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("كريدي")
            }
        }

        // Secondary actions
        SecondaryAction(
            text = "إضافة يوم عمل يدويًا",
            onClick = { onOpenWorkDayForm(-1L) },
        )
        SecondaryAction(
            text = "مشاركة الملخص",
            icon = Icons.Default.Share,
            onClick = { showShareDialog = true },
        )
        SecondaryAction(
            text = "الإحصائيات الشهرية",
            icon = Icons.Default.Insights,
            onClick = onOpenStats,
        )

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onOpenPayment,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
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

        Spacer(Modifier.height(8.dp))
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
private fun NetCard(net: Double, currency: String) {
    val accent = LocalExtendedColors.current.accent
    val border = LocalExtendedColors.current.border
    val muted = LocalExtendedColors.current.muted
    val isNegative = net < -0.0001

    val numberColor = if (isNegative) MaterialTheme.colorScheme.error else accent
    val borderColor = if (isNegative)
        MaterialTheme.colorScheme.error.copy(alpha = 0.30f)
    else accent.copy(alpha = 0.25f)

    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 24.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "الصافي الحالي",
            style = MaterialTheme.typography.labelLarge,
            color = muted,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = MoneyFormat.formatSigned(net, currency),
            style = MaterialTheme.typography.displaySmall,
            color = numberColor,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (isNegative) "الرصيد سالب" else "الرصيد موجب",
            style = MaterialTheme.typography.bodySmall,
            color = muted,
        )
    }
}

@Composable
private fun SummaryCard(
    totalMinutes: Long,
    totalWages: Double,
    totalRemote: Double,
    totalCredits: Double,
    carried: Double,
    currency: String,
    workDaysCount: Int,
) {
    val success = LocalExtendedColors.current.success

    AlamalCard {
        Text(
            text = "ملخّص الفترة",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        AlamalDivider()
        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCell(
                label = "الساعات",
                value = ArabicPlural.hoursMinutes(totalMinutes),
                modifier = Modifier.weight(1f),
            )
            StatCell(
                label = "عدد الأيام",
                value = ArabicPlural.days(workDaysCount.toLong()),
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCell(
                label = "أجور الساعات",
                value = MoneyFormat.format(totalWages, currency),
                modifier = Modifier.weight(1f),
            )
            if (totalRemote > 0.0001) {
                StatCell(
                    label = "منها عن بُعد",
                    value = MoneyFormat.format(totalRemote, currency),
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }

        if (totalCredits > 0.0001 || abs(carried) > 0.0001) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (totalCredits > 0.0001) {
                    StatCell(
                        label = "الكريدي",
                        value = "- " + MoneyFormat.format(totalCredits, currency),
                        valueColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (abs(carried) > 0.0001) {
                    StatCell(
                        label = "الرصيد المرحَّل",
                        value = MoneyFormat.formatSigned(carried, currency),
                        valueColor = if (carried < 0)
                            MaterialTheme.colorScheme.error
                        else success,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalExtendedColors.current.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = if (valueColor == Color.Unspecified)
                MaterialTheme.colorScheme.onSurface
            else valueColor,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ClockInCard(onClockIn: () -> Unit) {
    AlamalCard {
        Text(
            text = "لست في العمل الآن",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "اضغط الزر عند بدء العمل ليُسجَّل الوقت تلقائيًا.",
            style = MaterialTheme.typography.bodySmall,
            color = LocalExtendedColors.current.muted,
        )
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onClockIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("تسجيل وقت دخول العمل")
        }
    }
}

@Composable
private fun AtWorkCard(day: WorkDayEntity, onClockOut: () -> Unit) {
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
                    .clip(RoundedCornerShape(50))
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
            style = MaterialTheme.typography.bodyMedium,
            color = LocalExtendedColors.current.muted,
        )
        Text(
            text = "منذ ${ArabicPlural.hoursMinutes(elapsedMin)}",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onClockOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("تسجيل وقت الخروج")
        }
    }
}

@Composable
private fun SecondaryAction(
    text: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(text)
    }
}
