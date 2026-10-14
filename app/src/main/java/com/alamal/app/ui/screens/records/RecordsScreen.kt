package com.alamal.app.ui.screens.records

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alamal.app.ui.components.AlamalCard
import com.alamal.app.ui.components.AlamalDivider
import com.alamal.app.ui.theme.LocalExtendedColors
import com.alamal.app.util.DateFormatters
import com.alamal.app.util.MoneyFormat

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
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    bottom = 96.dp,
                ),
            ) {
                item {
                    Text(
                        text = "السجلات",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                items(state.weeks, key = { it.weekStart }) { week ->
                    WeekBlock(
                        week = week,
                        currency = state.currency,
                        onOpenWorkDay = onOpenWorkDay,
                        onOpenRemote = onOpenRemote,
                        onOpenCredit = onOpenCredit,
                    )
                }
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
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "ابدأ بتسجيل دخول العمل أو أضف عملًا عن بُعد / كريدي.",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalExtendedColors.current.muted,
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
    AlamalCard(contentPadding = 0) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = DateFormatters.weekLabel(week.weekStart, week.weekEnd),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = MoneyFormat.formatSigned(week.weekNet, currency),
                style = MaterialTheme.typography.titleMedium,
                color = if (week.weekNet < -0.0001)
                    MaterialTheme.colorScheme.error
                else LocalExtendedColors.current.success,
            )
        }
        AlamalDivider()
        // Rows
        week.items.forEachIndexed { index, item ->
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
            if (index < week.items.lastIndex) {
                AlamalDivider()
            }
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
    val amountColor = if (isCredit)
        MaterialTheme.colorScheme.error
    else
        MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon badge (rounded square with soft bg)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isCredit)
                    MaterialTheme.colorScheme.error
                else
                    LocalExtendedColors.current.accent,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = LocalExtendedColors.current.muted,
            )
            if (!item.note.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalExtendedColors.current.meta,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = amountText,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = amountColor,
        )
    }
}
