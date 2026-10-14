package com.alamal.app.ui.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlin.math.abs

@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 40.dp,
    visibleItems: Int = 3,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
    if (items.isEmpty()) return
    if (visibleItems < 1 || visibleItems % 2 == 0) return

    val halfVisible = visibleItems / 2
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    val padded: List<String?> = remember(items, halfVisible) {
        val left = List<String?>(halfVisible) { null }
        val middle: List<String?> = items
        val right = List<String?>(halfVisible) { null }
        left + middle + right
    }

    val safeIndex = selectedIndex.coerceIn(0, items.lastIndex)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeIndex)

    LaunchedEffect(safeIndex) {
        if (listState.firstVisibleItemIndex != safeIndex) {
            listState.animateScrollToItem(safeIndex)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                val idx = listState.firstVisibleItemIndex
                if (idx in items.indices) {
                    onSelectedIndexChange(idx)
                }
            }
    }

    val firstIdx by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val fraction by remember {
        derivedStateOf {
            if (itemHeightPx <= 0f) 0f
            else listState.firstVisibleItemScrollOffset / itemHeightPx
        }
    }

    Box(
        modifier = modifier.height(itemHeight * visibleItems),
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(
                items = padded,
                key = { idx, _ -> idx },
            ) { index, item ->
                if (item == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight),
                    )
                    return@itemsIndexed
                }

                val pos = (index - firstIdx) - fraction
                val distanceFromCenter = abs(pos - halfVisible)
                val maxDistance = halfVisible.toFloat().coerceAtLeast(1f)
                val normalized = (distanceFromCenter / maxDistance).coerceIn(0f, 1f)
                val scale = 1f - normalized * 0.35f
                val alphaValue = 1f - normalized * 0.75f
                val isSelected = index == firstIdx && fraction < 0.5f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item,
                        style = textStyle,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                alpha = alphaValue
                            },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.Center),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                )
            }
        }
    }
}
