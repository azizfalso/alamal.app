package com.alamal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alamal.app.ui.theme.LocalExtendedColors

/**
 * Claude-style card: ivory surface, 16dp radius, 1dp warm border, no
 * heavy shadow. Content padding tightened from 14dp to 12dp and inner
 * spacing from 8dp to 6dp (user request: "مظهر متزاحم بعض الشيء").
 */
@Composable
fun AlamalCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Int = 12,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = LocalExtendedColors.current.border
    val bg = MaterialTheme.colorScheme.surfaceContainerLow

    val base = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(bg)
        .border(1.dp, borderColor, shape)
        .then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )

    Column(
        modifier = base.padding(contentPadding.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/**
 * Subtle divider using the warm border color instead of pure gray.
 */
@Composable
fun AlamalDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalExtendedColors.current.border),
    )
}

/**
 * Page title, medium weight (never bold).
 */
@Composable
fun AlamalScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

/**
 * Label + value row, used in summary cards and stat blocks.
 * Vertical padding tightened from 2dp to 0dp so rows stack compactly.
 */
@Composable
fun AlamalStatRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    bold: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalExtendedColors.current.muted,
        )
        Spacer(Modifier.padding(horizontal = 8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium,
            color = if (valueColor == Color.Unspecified)
                MaterialTheme.colorScheme.onSurface
            else valueColor,
        )
    }
}

/**
 * Small chip-like label, e.g. "منها عن بُعد" or a period date range.
 */
@Composable
fun AlamalPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Draws a dashed rounded border around the composable.
 * Used for the "clock in" quick action, signalling an unfilled state.
 *
 * The rect is inset by half the stroke width so the full stroke stays visible
 * even when the composable is clipped to the same rounded shape.
 */
fun Modifier.dashedBorder(
    color: Color,
    strokeWidth: Dp = 1.dp,
    cornerRadius: Dp = 16.dp,
    dashLength: Dp = 8.dp,
    gapLength: Dp = 6.dp,
): Modifier = this.drawBehind {
    val strokeWidthPx = strokeWidth.toPx()
    val inset = strokeWidthPx / 2f
    val cornerRadiusPx = cornerRadius.toPx()
    val dashLengthPx = dashLength.toPx()
    val gapLengthPx = gapLength.toPx()

    val path = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(
                    inset,
                    inset,
                    size.width - inset,
                    size.height - inset,
                ),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            )
        )
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidthPx,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(dashLengthPx, gapLengthPx),
                0f,
            ),
        ),
    )
}
