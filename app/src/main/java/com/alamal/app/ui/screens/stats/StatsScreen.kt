package com.alamal.app.ui.screens.stats

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alamal.app.util.ArabicPlural
import com.alamal.app.util.MoneyFormat

private val PositiveGreen = Color(0xFF2E7D32)
private val NegativeRed = Color(0xFFB8322E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: StatsViewModel = viewModel(factory = StatsViewModel.factory(context))
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإحصائيات الشهرية") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Month navigation
            Card(modifier = Modifier.fillMaxWidth()) {
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

            // Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatRow("أيام العمل", state.daysCount.toString())
                    StatRow("الساعات", ArabicPlural.hoursMinutes(state.totalMinutes))
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
                    HorizontalDivider()
                    StatRow(
                        "الصافي",
                        MoneyFormat.formatSigned(state.net, state.currency),
                        valueColor = colorOf(state.net),
                        bold = true,
                    )
                }
            }

            // Bar chart
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "المبالغ اليومية",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "الأعمدة الخضراء مبالغ موجبة، والحمراء كريدي/سالب",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    BarChart(
                        bars = state.dailyBars,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                    )
                    Spacer(Modifier.height(4.dp))
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
        val scale = (h / 2f) * 0.9f / maxAbs.toFloat()

        bars.forEachIndexed { i, bar ->
            val x = i * (barW + gap)
            val barH = (kotlin.math.abs(bar.amount).toFloat() * scale).coerceAtMost(h / 2f * 0.95f)
            if (barH > 0.5f) {
                if (bar.amount >= 0) {
                    drawRect(
                        color = PositiveGreen,
                        topLeft = Offset(x, zeroY - barH),
                        size = Size(barW, barH),
                    )
                } else {
                    drawRect(
                        color = NegativeRed,
                        topLeft = Offset(x, zeroY),
                        size = Size(barW, barH),
                    )
                }
            }
        }

        drawLine(
            color = Color(0xFF888888),
            start = Offset(0f, zeroY),
            end = Offset(w, zeroY),
            strokeWidth = 1f,
        )
    }
}

@Composable
private fun colorOf(v: Double): Color = when {
    v < -0.0001 -> MaterialTheme.colorScheme.error
    v > 0.0001 -> PositiveGreen
    else -> MaterialTheme.colorScheme.onSurface
}
