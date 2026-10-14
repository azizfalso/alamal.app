package com.alamal.app.ui.screens.stats

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alamal.app.util.ArabicPlural
import com.alamal.app.util.MoneyFormat

private val PositiveGreen = Color(0xFF2E7D32)
private val NegativeRed = Color(0xFFB8322E)

private val DayNamesAr = listOf("إث", "ثل", "أر", "خم", "جم", "سب", "أح")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: StatsViewModel = viewModel(factory = StatsViewModel.factory(context))
    val state by vm.state.collectAsState()

    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإحصائيات") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                        )
                    }
                },
            )
        },
    ) { pad ->
        if (state.loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Month navigation
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { vm.goPrev() }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "الشهر السابق")
                    }
                    Text(
                        text = state.monthLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(
                        onClick = { vm.goNext() },
                        enabled = state.canGoNext,
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "الشهر التالي")
                    }
                }
            }

            // Segmented tabs
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("نظرة عامة") }
                SegmentedButton(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("بالأسبوع") }
            }

            if (tab == 0) {
                OverviewTab(state)
            } else {
                WeeklyTab(state)
            }
        }
    }
}

@Composable
private fun OverviewTab(state: StatsUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Net card — big
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (state.net < -0.0001)
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
                    text = "صافي الشهر",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = MoneyFormat.formatSigned(state.net, state.currency),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (state.net < -0.0001)
                        MaterialTheme.colorScheme.error
                    else PositiveGreen,
                )
            }
        }

        // Stat grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatRow("أيام العمل", state.daysCount.toString())
                StatRow("ساعات العمل", ArabicPlural.hoursMinutes(state.totalMinutes))
                StatRow("أجور الساعات", MoneyFormat.format(state.totalWages, state.currency))
                if (state.totalRemote > 0.0001) {
                    StatRow("منها عن بُعد", MoneyFormat.format(state.totalRemote, state.currency))
                }
                if (state.totalCredits > 0.0001) {
                    StatRow(
                        "الكريدي",
                        "- " + MoneyFormat.format(state.totalCredits, state.currency),
                        valueColor = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        // Daily bars
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "المبالغ اليومية",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                BarChart(
                    bars = state.dailyBars,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("1", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${state.dailyBars.size}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyTab(state: StatsUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.weeklyStats.isEmpty()) {
            Text(
                text = "لا توجد بيانات لهذا الشهر.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }

        state.weeklyStats.forEachIndexed { index, week ->
            WeekCard(
                week = week,
                index = index + 1,
                currency = state.currency,
            )
        }
    }
}

@Composable
private fun WeekCard(week: WeekStats, index: Int, currency: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "الأسبوع $index",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = week.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider()

            // Totals
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "الساعات",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = ArabicPlural.hoursMinutes(week.totalMinutes),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "المبلغ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = MoneyFormat.formatSigned(week.totalAmount, currency),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (week.totalAmount < -0.0001)
                            MaterialTheme.colorScheme.error
                        else PositiveGreen,
                    )
                }
            }

            // 7 mini bars with day labels
            MiniWeekBars(days = week.days, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MiniWeekBars(days: List<DayBar>, modifier: Modifier = Modifier) {
    val maxAbs = days.maxOfOrNull { kotlin.math.abs(it.amount) } ?: 0.0
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
        ) {
            val w = size.width
            val h = size.height
            val count = 7
            val gap = 8f
            val barW = ((w - gap * (count - 1)) / count).coerceAtLeast(1f)
            val zeroY = h - 6f
            val maxBarH = h - 14f

            days.forEachIndexed { i, bar ->
                val x = i * (barW + gap)
                if (maxAbs > 0.0001) {
                    val ratio = kotlin.math.abs(bar.amount).toFloat() / maxAbs.toFloat()
                    val barH = (ratio * maxBarH).coerceAtLeast(2f)
                    val color = if (bar.amount < -0.0001) NegativeRed else PositiveGreen
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, zeroY - barH),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barW / 3f, barW / 3f),
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            days.forEachIndexed { i, _ ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = DayNamesAr.getOrElse(i) { "" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRow(
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
private fun BarChart(bars: List<DayBar>, modifier: Modifier = Modifier) {
    val maxAbs = bars.maxOfOrNull { kotlin.math.abs(it.amount) } ?: 0.0
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val count = bars.size
        if (count == 0) return@Canvas

        val zeroY = h / 2f
        if (maxAbs < 0.0001) {
            drawLine(
                color = Color(0xFF888888),
                start = Offset(0f, zeroY),
                end = Offset(w, zeroY),
                strokeWidth = 1f,
            )
            return@Canvas
        }

        val gap = 2f
        val barW = ((w - gap * (count - 1)) / count).coerceAtLeast(1f)
        val scale = (h / 2f) * 0.85f / maxAbs.toFloat()
        val radius = CornerRadius(barW / 2f, barW / 2f)

        bars.forEachIndexed { i, bar ->
            val x = i * (barW + gap)
            val barH = (kotlin.math.abs(bar.amount).toFloat() * scale).coerceAtMost(h / 2f * 0.9f)
            if (barH > 0.5f) {
                if (bar.amount >= 0) {
                    drawRoundRect(
                        color = PositiveGreen,
                        topLeft = Offset(x, zeroY - barH),
                        size = Size(barW, barH),
                        cornerRadius = radius,
                    )
                } else {
                    drawRoundRect(
                        color = NegativeRed,
                        topLeft = Offset(x, zeroY),
                        size = Size(barW, barH),
                        cornerRadius = radius,
                    )
                }
            }
        }

        drawLine(
            color = Color(0xFF888888).copy(alpha = 0.5f),
            start = Offset(0f, zeroY),
            end = Offset(w, zeroY),
            strokeWidth = 1f,
        )
    }
}
