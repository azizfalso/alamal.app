package com.alamal.app.ui.screens.records

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                // ---- Header ----
                item {
                    Text(
                        text = "السجلات",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // ---- Search bar ----
                item {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = vm::setSearchQuery,
                        placeholder = { Text("ابحث في الملاحظات...") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = LocalExtendedColors.current.muted,
                            )
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { vm.setSearchQuery("") }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "مسح",
                                        tint = LocalExtendedColors.current.muted,
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // ---- Filter chips ----
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = state.filter == RecordFilter.ALL,
                            onClick = { vm.setFilter(RecordFilter.ALL) },
                            label = { Text("الكل") },
                        )
                        FilterChip(
                            selected = state.filter == RecordFilter.WORK,
                            onClick = { vm.setFilter(RecordFilter.WORK) },
                            label = { Text("عمل") },
                        )
                        FilterChip(
                            selected = state.filter == RecordFilter.REMOTE,
                            onClick = { vm.setFilter(RecordFilter.REMOTE) },
                            label = { Text("عن بُعد") },
                        )
                        FilterChip(
                            selected = state.filter == RecordFilter.CREDIT,
                            onClick = { vm.setFilter(RecordFilter.CREDIT) },
                            label = { Text("كريدي") },
                        )
                    }
                }

                // ---- Empty state (no results after filtering) ----
                if (state.weeks.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = if (state.searchQuery.isNotEmpty() || state.filter != RecordFilter.ALL)
                                    "لا توجد نتائج مطابقة"
                                else
                                    "لا توجد سجلات بعد",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (state.searchQuery.isNotEmpty() || state.filter != RecordFilter.ALL)
                                    "جرّب تغيير كلمة البحث أو نوع الفلتر."
                                else
                                    "ابدأ بتسجيل دخول العمل أو أضف عملًا عن بُعد / كريدي.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LocalExtendedColors.current.muted,
                            )
                        }
                    }
                }

                // ---- Results count ----
                if (state.weeks.isNotEmpty() &&
                    (state.searchQuery.isNotEmpty() || state.filter != RecordFilter.ALL)
                ) {
                    item {
                        Text(
                            text = "${state.totalMatches} نتيجة",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalExtendedColors.current.muted,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }

                // ---- Weeks ----
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
private fun WeekBlock(
    week: WeekGroup,
    currency: String,
    onOpenWorkDay: (Long) -> Unit,
    onOpenRemote: (Long) -> Unit,
    onOpenCredit: (Long) -> Unit,
) {
    AlamalCard(contentPadding = 0) {
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
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
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
                fontWeight = FontWeight.Medium,
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
            style = MaterialTheme.typography.titleMedium,
            color = amountColor,
        )
    }
}
