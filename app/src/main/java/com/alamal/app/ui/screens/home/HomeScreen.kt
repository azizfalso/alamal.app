package com.alamal.app.ui.screens.home

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alamal.app.data.db.entity.WorkDayEntity
import com.alamal.app.ui.components.AlamalCard
import com.alamal.app.ui.components.AlamalDivider
import com.alamal.app.ui.components.AlamalStatRow
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "الفترة الحالية",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SecondaryAction(
                text = "عمل عن بُعد",
                icon = Icons.Default.Laptop,
                modifier = Modifier.weight(1f),
                onClick = onOpenRemoteWorkForm,
            )
            SecondaryAction(
                text = "كريدي",
                icon = Icons.Default.MoneyOff,
                modifier = Modifier.weight(1f),
                onClick = onOpenCreditForm,
            )
        }

        SecondaryAction(
            text = "إضافة يوم عمل يدويًا",
            onClick = { onOpenWorkDayForm(-1L) },
        )
        SecondaryAction(
            text = "مشاركة ملخص الفترة الحالية",
            icon = Icons.Default.Share,
            onClick = { showShareDialog = true },
        )
        SecondaryAction(
            text = "الإحصائيات الشهرية",
            icon = Icons.Default.Insights,
            onClick = onOpenStats,
        )

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
    val accentOn = LocalExtendedColors.current.accentOn
    val isNegative = net < -0.0001
    val bg = if (isNegative) MaterialTheme.colorScheme.errorContainer else accent
    val fg = if (isNegative) MaterialTheme.colorScheme.error else accentOn
    val labelColor = if (isNegative)
        MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
    else accentOn.copy(alpha = 0.85f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "الصافي الحالي",
                style = MaterialTheme.typography.labelLarge,
                color = labelColor,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = MoneyFormat.formatSigned(net, currency),
                style = MaterialTheme.typography.displaySmall,
                color = fg,
                fontWeight = FontWeight.Medium,
            )
        }
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
    AlamalCard {
        Text(
            text = "ملخّص الفترة",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        AlamalDivider()
        AlamalStatRow("الساعات", ArabicPlural.hoursMinutes(totalMinutes))
        AlamalStatRow("عدد الأيام", ArabicPlural.days(workDaysCount.toLong()))
        AlamalStatRow("أجور الساعات", MoneyFormat.format(totalWages, currency))
        if (totalRemote > 0.0001) {
            AlamalStatRow("منها عن بُعد", MoneyFormat.format(totalRemote, currency))
        }
        if (totalCredits > 0.0001) {
            AlamalStatRow(
                label = "الكريدي",
                value = "- " + MoneyFormat.format(totalCredits, currency),
                valueColor = MaterialTheme.colorScheme.error,
            )
        }
        if (kotlin.math.abs(carried) > 0.0001) {
            AlamalStatRow(
                label = "الرصيد المرحَّل",
                value = MoneyFormat.formatSigned(carried, currency),
                valueColor = if (carried < 0)
                    MaterialTheme.colorScheme.error
                else LocalExtendedColors.current.success,
            )
        }
    }
}

@Composable
private fun ClockInCard(onClockIn: () -> Unit) {
    AlamalCard {
        Text(
            text = "لست في العمل الآن",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "اضغط الزر عند بدء العمل ليُسجَّل الوقت تلقائيًا.",
            style = MaterialTheme.typography.bodySmall,
            color = LocalExtendedColors.current.muted,
        )
        Spacer(Modifier.height(2.dp))
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

    AlamalCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(LocalExtendedColors.current.success),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "أنت الآن في العمل",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
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
        )
        Spacer(Modifier.height(2.dp))
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
