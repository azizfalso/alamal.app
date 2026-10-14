package com.alamal.app.ui.screens.records

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alamal.app.util.DateFormatters
import com.alamal.app.util.MoneyFormat

private val PositiveGreen = Color(0xFF2E7D32)

@Composable
fun RecordsScreen(
    onOpenWorkDay: (Long) -> Unit,
    onOpenRemote: (Long) -> Unit,
    onOpenCredit: (Long) -> Unit,
) {
    val context = LocalContext.current
    val vm: RecordsViewModel = viewModel(factory = RecordsViewModel.factory(context))
    val state by vm.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.weeks.isEmpty() -> EmptyState()

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.weeks, key = { it.weekStart }) { week ->
                    WeekBlock(
                        week = week,
                        currency = state.currency,
                        onOpenWorkDay = onOpenWorkDay,
                        onOpenRemote = onOpenRemote,
                        onOpenCredit = onOpenCredit,
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
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
            text = "لا توجد سجلات بعد",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "ابدأ بتسجيل دخول العمل أو أضف عملًا عن بُعد/كريدي.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WeekBlock(
    week: WeekGroup,
    currency: String,
    onOpenWorkDay: (Long) -> Unit,
    onOpenRemote: (Long) -> Unit,
    onOpenCredit: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = DateFormatters.weekLabel(week.weekStart, week.weekEnd),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "صافي: ${MoneyFormat.formatSigned(week.weekNet, currency)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (week.weekNet < -0.0001) MaterialTheme.colorScheme.error else PositiveGreen,
                )
            }
            HorizontalDivider()
            week.items.forEach { item ->
                RecordRow(
                    item = item,
                    currency = currency,
                    onClick = {
                        when (item.type) {
                            RecordType.WORK_DAY -> onOpenWorkDay(item.id)
                            RecordType.REMOTE -> onOpenRemote(item.id)
                            RecordType.CREDIT -> onOpenCredit(item.id)
                        }
                    },
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun RecordRow(
    item: RecordItem,
    currency: String,
    onClick: () -> Unit,
) {
    val icon = when (item.type) {
        RecordType.WORK_DAY -> Icons.Default.Schedule
        RecordType.REMOTE -> Icons.Default.Home
        RecordType.CREDIT -> Icons.Default.MoneyOff
    }
    val isCredit = item.type == RecordType.CREDIT
    val amountText = if (isCredit)
        "- " + MoneyFormat.format(item.amount, currency)
    else MoneyFormat.format(item.amount, currency)
    val amountColor = if (isCredit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!item.note.isNullOrBlank()) {
                Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = amountText,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = amountColor,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}
