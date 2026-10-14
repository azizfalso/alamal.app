package com.alamal.app.ui.screens.lock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alamal.app.R
import com.alamal.app.ui.theme.LocalExtendedColors
import kotlinx.coroutines.delay

@Composable
fun LockScreen(
    expectedPin: String,
    onUnlock: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(error) {
        if (error) {
            repeat(3) {
                shakeOffset.animateTo(18f, tween(50))
                shakeOffset.animateTo(-18f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))
            error = false
            input = ""
        }
    }

    LaunchedEffect(input) {
        if (input.length == 4) {
            if (input == expectedPin) {
                delay(100)
                onUnlock()
            } else {
                error = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Lock icon (a simple soft badge)
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    width = 1.dp,
                    color = LocalExtendedColors.current.border,
                    shape = RoundedCornerShape(20.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "🔒",
                fontSize = 28.sp,
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.lock_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.lock_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalExtendedColors.current.muted,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(44.dp))

        // 4 PIN dots
        Row(
            modifier = Modifier.graphicsLayer { translationX = shakeOffset.value },
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            repeat(4) { i ->
                val filled = i < input.length
                val dotColor = when {
                    error -> MaterialTheme.colorScheme.error
                    filled -> LocalExtendedColors.current.accent
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                }
                val dotSize = if (filled) 20.dp else 16.dp
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(
                            width = if (!filled && !error) 1.dp else 0.dp,
                            color = if (!filled && !error)
                                LocalExtendedColors.current.borderStrong
                            else
                                dotColor,
                            shape = CircleShape,
                        )
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.lock_error),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.alpha(if (error) 1f else 0f),
        )

        Spacer(Modifier.height(36.dp))

        // Keypad
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "\u232B"),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            keys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    row.forEach { key ->
                        KeypadButton(
                            label = key,
                            onClick = {
                                when (key) {
                                    "" -> Unit
                                    "\u232B" -> if (input.isNotEmpty()) input = input.dropLast(1)
                                    else -> if (input.length < 4) input += key
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(label: String, onClick: () -> Unit) {
    val isEmpty = label.isEmpty()
    val isBackspace = label == "\u232B"

    val bgColor = when {
        isEmpty -> MaterialTheme.colorScheme.background
        isBackspace -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val borderColor = when {
        isEmpty -> androidx.compose.ui.graphics.Color.Transparent
        else -> LocalExtendedColors.current.border
    }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = CircleShape,
            )
            .clickable(enabled = !isEmpty) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        when {
            isBackspace -> Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = stringResource(R.string.delete),
                tint = LocalExtendedColors.current.muted,
                modifier = Modifier.size(24.dp),
            )
            label.isNotEmpty() -> Text(
                text = label,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
