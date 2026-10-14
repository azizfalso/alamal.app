package com.alamal.app.ui.screens.home

import android.content.Intent
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alamal.app.data.db.entity.WorkDayEntity
import com.alamal.app.ui.components.SummaryShareDialog
import com.alamal.app.util.ArabicPlural
import com.alamal.app.util.DateFormatters
import com.alamal.app.util.MoneyFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PositiveGreen = Color(0xFF2E7D32)

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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "الفترة الحالية",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )

        NetCard(net = state.netBalance, currency = state.currency)

        SummaryRow(
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
            AtWorkCard(
                day = open,
                onClockOut = { onOpenWorkDayForm(open.id) },
            )
        } else {
            ClockInCard(onClockIn = { vm.clockIn() })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalButton(
                onClick = onOpenRemoteWorkForm,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Laptop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("عمل عن بُعد")
            }
            FilledTonalButton(
                onClick = onOpenCreditForm,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.MoneyOff, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("كريدي")
            }
        }

        OutlinedButton(
            onClick = { onOpenWorkDayForm(-1L) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("إضافة يوم عمل يدويًا")
        }

        OutlinedButton(
            onClick = { showShareDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("مشاركة ملخص الفترة الحالية")
        }

        OutlinedButton(
            onClick = onOpenStats,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("الإحصائيات الشهرية")
        }

        Button(
            onClick = onOpenPayment,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("استلمت الدفعة", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(4.dp))
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
    val isNegative = net < -0.0001
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isNegative)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "الصافي الحالي",
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = MoneyFormat.formatSigned(net, currency),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isNegative) MaterialTheme.colorScheme.error else PositiveGreen,
            )
        }
    }
}

@Composable
private fun SummaryRow(
    totalMinutes: Long,
    totalWages: Double,
    totalRemote: Double,
    totalCredits: Double,
    carried: Double,
    currency: String,
    workDaysCount: Int,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatLine("الساعات", ArabicPlural.hoursMinutes(totalMinutes))
            StatLine("عدد الأيام", ArabicPlural.days(workDaysCount.toLong()))
            StatLine("أجور الساعات", MoneyFormat.format(totalWages, currency))
            if (totalRemote > 0.0001) {
                StatLine("منها عن بُعد", MoneyFormat.format(totalRemote, currency))
            }
            if (totalCredits > 0.0001) {
                StatLine(
                    "الكريدي",
                    "- " + MoneyFormat.format(totalCredits, currency),
                    valueColor = MaterialTheme.colorScheme.error,
                )
            }
            if (kotlin.math.abs(carried) > 0.0001) {
                StatLine(
                    "الرصيد المرحَّل",
                    MoneyFormat.formatSigned(carried, currency),
                    valueColor = if (carried < 0) MaterialTheme.colorScheme.error else PositiveGreen,
                )
            }
        }
    }
}

@Composable
private fun StatLine(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor,
        )
    }
}

@Composable
private fun ClockInCard(onClockIn: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "لست في العمل الآن",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Button(
                onClick = onClockIn,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("تسجيل وقت دخول العمل")
            }
        }
    }
}

@Composable
private fun AtWorkCard(
    day: WorkDayEntity,
    onClockOut: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            now = System.currentTimeMillis()
        }
    }
    val elapsedMs = (now - day.clockIn).coerceAtLeast(0L)
    val elapsedMin = elapsedMs / 60_000L

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "أنت الآن في العمل",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "بدأت في ${DateFormatters.time(day.clockIn)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "منذ ${ArabicPlural.hoursMinutes(elapsedMin)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Button(
                onClick = onClockOut,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("تسجيل وقت الخروج")
            }
        }
    }
}
