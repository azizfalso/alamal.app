package com.alamal.app.ui.screens.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.alamal.app.R
import com.alamal.app.data.AppRepository
import com.alamal.app.data.PeriodSnapshot
import com.alamal.app.data.prefs.AppSettings
import com.alamal.app.data.prefs.SettingsRepository
import com.alamal.app.util.ArabicPlural
import com.alamal.app.util.DateFormatters
import com.alamal.app.util.MoneyFormat
import kotlinx.coroutines.launch
import java.util.Locale

private val PositiveGreen = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { AppRepository.get(context) }
    val settingsRepo = remember { SettingsRepository(context) }
    val settings by settingsRepo.settings.collectAsState(initial = AppSettings.DEFAULT)

    var snapshot by remember { mutableStateOf<PeriodSnapshot?>(null) }
    var loading by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val snap = repo.computeCurrentSnapshot()
        snapshot = snap
        if (!initialized) {
            amountText = formatAmount(snap.amountDue)
            initialized = true
        }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("استلام الدفعة") },
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
        val snap = snapshot
        if (loading || snap == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val currency = settings.currency
        val received = amountText.toDoubleOrNull() ?: 0.0
        val difference = snap.amountDue - received
        val hasAnything = snap.workDays.isNotEmpty() ||
            snap.remoteWorks.isNotEmpty() ||
            snap.credits.isNotEmpty() ||
            kotlin.math.abs(snap.carriedBalance) > 0.0001

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "الفترة",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${DateFormatters.dateArabicShort(snap.startDate)} ← ${DateFormatters.dateArabicShort(snap.endDate)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "الأيام: ${ArabicPlural.days(snap.workDays.count { it.clockOut != null }.toLong())}" +
                            " • " + ArabicPlural.hoursMinutes(snap.totalMinutes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "التدقيق",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    CalcLine(
                        label = "الرصيد المرحَّل",
                        value = MoneyFormat.formatSigned(snap.carriedBalance, currency),
                        valueColor = colorOf(snap.carriedBalance),
                    )
                    CalcLine(
                        label = "أجور الساعات",
                        value = MoneyFormat.format(snap.totalWages, currency),
                    )
                    if (snap.totalRemote > 0.0001) {
                        CalcLine(
                            label = "منها عن بُعد",
                            value = MoneyFormat.format(snap.totalRemote, currency),
                        )
                    }
                    if (snap.totalCredits > 0.0001) {
                        CalcLine(
                            label = "الكريدي",
                            value = "- " + MoneyFormat.format(snap.totalCredits, currency),
                            valueColor = MaterialTheme.colorScheme.error,
                        )
                    }
                    HorizontalDivider()
                    CalcLine(
                        label = "المستحق",
                        value = MoneyFormat.formatSigned(snap.amountDue, currency),
                        valueColor = colorOf(snap.amountDue),
                        bold = true,
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("المبلغ المستلم ($currency)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("ملاحظة (اختياري)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (difference < -0.0001)
                        MaterialTheme.colorScheme.errorContainer
                    else if (difference > 0.0001)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = when {
                            difference > 0.0001 -> "سيبقى في الحساب"
                            difference < -0.0001 -> "سيكون رصيدًا زائدًا"
                            else -> "الحساب سيتصفّر"
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = MoneyFormat.formatSigned(difference, currency),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorOf(difference),
                    )
                    Text(
                        text = "سيُرحَّل هذا المبلغ للفترة القادمة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!hasAnything) {
                Text(
                    text = "لا توجد سجلات ولا رصيد مرحَّل. تأكد من وجود أيام عمل أو مبالغ قبل استلام دفعة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

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
                        if (saving) return@Button
                        saving = true
                        scope.launch {
                            repo.receivePayment(
                                amountReceived = received,
                                note = noteText.ifBlank { null },
                            )
                            saving = false
                            onDone()
                        }
                    },
                    enabled = !saving && hasAnything,
                    modifier = Modifier.weight(1f),
                ) { Text("تأكيد الدفعة") }
            }
        }
    }
}

@Composable
private fun CalcLine(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    bold: Boolean = false,
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
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor,
        )
    }
}

@Composable
private fun colorOf(value: Double): Color = when {
    value < -0.0001 -> MaterialTheme.colorScheme.error
    value > 0.0001 -> PositiveGreen
    else -> MaterialTheme.colorScheme.onSurface
}

private fun formatAmount(a: Double): String {
    val rounded = Math.round(a * 100.0) / 100.0
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString()
    else String.format(Locale.US, "%.2f", rounded)
}
