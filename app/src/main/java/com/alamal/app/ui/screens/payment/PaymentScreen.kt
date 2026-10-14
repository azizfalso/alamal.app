package com.alamal.app.ui.screens.payment

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
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
import com.alamal.app.ui.components.AlamalCard
import com.alamal.app.ui.components.AlamalDivider
import com.alamal.app.ui.components.AlamalStatRow
import com.alamal.app.ui.theme.LocalExtendedColors
import com.alamal.app.util.ArabicPlural
import com.alamal.app.util.DateFormatters
import com.alamal.app.util.MoneyFormat
import kotlinx.coroutines.launch
import java.util.Locale

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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
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
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "${DateFormatters.dateArabicShort(snap.startDate)} ← ${DateFormatters.dateArabicShort(snap.endDate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalExtendedColors.current.muted,
            )

            AmountDueCard(amountDue = snap.amountDue, currency = currency)

            AlamalCard {
                Text(
                    text = "التدقيق",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                AlamalDivider()
                AlamalStatRow(
                    "الرصيد المرحَّل",
                    MoneyFormat.formatSigned(snap.carriedBalance, currency),
                    valueColor = colorOf(snap.carriedBalance),
                )
                AlamalStatRow("أجور الساعات", MoneyFormat.format(snap.totalWages, currency))
                if (snap.totalRemote > 0.0001) {
                    AlamalStatRow("منها عن بُعد", MoneyFormat.format(snap.totalRemote, currency))
                }
                if (snap.totalCredits > 0.0001) {
                    AlamalStatRow(
                        "الكريدي",
                        "- " + MoneyFormat.format(snap.totalCredits, currency),
                        valueColor = MaterialTheme.colorScheme.error,
                    )
                }
                AlamalStatRow(
                    "عدد الأيام",
                    ArabicPlural.days(snap.workDays.count { it.clockOut != null }.toLong()),
                )
                AlamalStatRow(
                    "الساعات",
                    ArabicPlural.hoursMinutes(snap.totalMinutes()),
                )
            }

            AlamalCard {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ المستلم ($currency)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("ملاحظة (اختياري)") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            DifferenceCard(difference = difference, currency = currency)

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
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
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
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("تأكيد الدفعة")
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AmountDueCard(amountDue: Double, currency: String) {
    val accent = LocalExtendedColors.current.accent
    val accentOn = LocalExtendedColors.current.accentOn
    val isNegative = amountDue < -0.0001

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
                text = "المستحق",
                style = MaterialTheme.typography.labelLarge,
                color = labelColor,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = MoneyFormat.formatSigned(amountDue, currency),
                style = MaterialTheme.typography.displaySmall,
                color = fg,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun DifferenceCard(difference: Double, currency: String) {
    val isNegative = difference < -0.0001
    val isPositive = difference > 0.0001

    val bg = when {
        isNegative -> MaterialTheme.colorScheme.errorContainer
        isPositive -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val label = when {
        isPositive -> "سيبقى في الحساب"
        isNegative -> "سيكون رصيدًا زائدًا"
        else -> "الحساب سيتصفّر"
    }
    val fg = when {
        isNegative -> MaterialTheme.colorScheme.error
        isPositive -> LocalExtendedColors.current.success
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = LocalExtendedColors.current.muted,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = MoneyFormat.formatSigned(difference, currency),
                style = MaterialTheme.typography.headlineMedium,
                color = fg,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "سيُرحَّل هذا المبلغ للفترة القادمة",
                style = MaterialTheme.typography.bodySmall,
                color = LocalExtendedColors.current.muted,
            )
        }
    }
}

@Composable
private fun colorOf(value: Double): Color = when {
    value < -0.0001 -> MaterialTheme.colorScheme.error
    value > 0.0001 -> LocalExtendedColors.current.success
    else -> MaterialTheme.colorScheme.onSurface
}

private fun formatAmount(a: Double): String {
    val rounded = Math.round(a * 100.0) / 100.0
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString()
    else String.format(Locale.US, "%.2f", rounded)
}
