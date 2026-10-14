package com.alamal.app.ui.screens.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.alamal.app.data.prefs.AppSettings
import com.alamal.app.data.prefs.SettingsRepository
import com.alamal.app.data.prefs.ThemeMode
import com.alamal.app.ui.components.ColorPickerDialog
import com.alamal.app.ui.theme.parseHexColor
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsState(initial = AppSettings.DEFAULT)

    var showColorPicker by remember { mutableStateOf(false) }
    val currentColor = parseHexColor(settings.appColorHex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )

        SectionCard(title = stringResource(R.string.settings_section_appearance)) {
            Text(
                text = stringResource(R.string.settings_theme_mode),
                style = MaterialTheme.typography.titleSmall,
            )
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { scope.launch { repo.setThemeMode(mode) } }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = settings.themeMode == mode,
                            onClick = { scope.launch { repo.setThemeMode(mode) } },
                        )
                        Text(
                            text = when (mode) {
                                ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                                ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                                ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.settings_color),
                style = MaterialTheme.typography.titleSmall,
            )

            // Preview + "change" button — no hex input.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(currentColor)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(14.dp),
                        ),
                )
                Button(
                    onClick = { showColorPicker = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("تغيير اللون")
                }
            }
        }

        SectionCard(title = stringResource(R.string.settings_section_money)) {
            var currency by remember(settings.currency) { mutableStateOf(settings.currency) }
            OutlinedTextField(
                value = currency,
                onValueChange = { currency = it },
                label = { Text(stringResource(R.string.settings_currency)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            var rate by remember(settings.hourlyRate) {
                mutableStateOf(
                    if (settings.hourlyRate == settings.hourlyRate.toLong().toDouble())
                        settings.hourlyRate.toLong().toString()
                    else settings.hourlyRate.toString()
                )
            }
            OutlinedTextField(
                value = rate,
                onValueChange = { rate = it },
                label = { Text(stringResource(R.string.settings_hourly_rate)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        repo.setCurrency(currency.ifBlank { "DH" })
                        rate.toDoubleOrNull()?.let { repo.setHourlyRate(it) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save))
            }
        }

        BackupSettingsSection()

        SectionCard(title = stringResource(R.string.settings_section_security)) {
            PinManager(repo = repo, currentPin = settings.pin)
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_lock_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(title = stringResource(R.string.settings_section_about)) {
            val versionName = rememberVersionName(context)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.settings_version))
                Text(versionName, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_part1_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = currentColor,
            onDismiss = { showColorPicker = false },
            onConfirm = { picked ->
                val hex = String.format("#%06X", 0xFFFFFF and picked.toArgb())
                scope.launch { repo.setAppColorHex(hex) }
                showColorPicker = false
            },
        )
    }
}

@Suppress("DEPRECATION")
private fun rememberVersionName(context: Context): String {
    return try {
        val pm: PackageManager = context.packageManager
        val info = pm.getPackageInfo(context.packageName, 0)
        info.versionName ?: "?"
    } catch (_: Exception) {
        "?"
    }
}

@Composable
internal fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

private enum class PinAction { SET, CHANGE, REMOVE }

private enum class PinStage { CURRENT, NEW, CONFIRM }

@Composable
private fun PinManager(
    repo: SettingsRepository,
    currentPin: String,
) {
    val scope = rememberCoroutineScope()
    var action by remember { mutableStateOf<PinAction?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (currentPin.isEmpty()) {
            Button(
                onClick = { action = PinAction.SET },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_lock_set))
            }
        } else {
            OutlinedButton(
                onClick = { action = PinAction.CHANGE },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_lock_change))
            }
            OutlinedButton(
                onClick = { action = PinAction.REMOVE },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_lock_remove))
            }
        }
    }

    action?.let { a ->
        PinDialog(
            action = a,
            currentPin = currentPin,
            onDismiss = { action = null },
            onConfirm = { newPin ->
                scope.launch { repo.setPin(newPin) }
                action = null
            },
        )
    }
}

@Composable
private fun PinDialog(
    action: PinAction,
    currentPin: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val context = LocalContext.current
    var stage by remember(action) {
        mutableStateOf(if (action == PinAction.SET) PinStage.NEW else PinStage.CURRENT)
    }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var currentEntry by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun msg(resId: Int) = context.getString(resId)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (action) {
                    PinAction.SET -> stringResource(R.string.settings_lock_set)
                    PinAction.CHANGE -> stringResource(R.string.settings_lock_change)
                    PinAction.REMOVE -> stringResource(R.string.settings_lock_remove)
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (stage) {
                    PinStage.CURRENT -> OutlinedTextField(
                        value = currentEntry,
                        onValueChange = { currentEntry = it.filter(Char::isDigit).take(4) },
                        label = { Text(stringResource(R.string.settings_lock_enter_current)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                    PinStage.NEW -> OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it.filter(Char::isDigit).take(4) },
                        label = { Text(stringResource(R.string.settings_lock_enter_new)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                    PinStage.CONFIRM -> OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it.filter(Char::isDigit).take(4) },
                        label = { Text(stringResource(R.string.settings_lock_confirm_new)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    error = null
                    when (stage) {
                        PinStage.CURRENT -> {
                            if (currentEntry != currentPin) {
                                error = msg(R.string.settings_lock_wrong_current)
                            } else if (action == PinAction.REMOVE) {
                                onConfirm("")
                            } else {
                                stage = PinStage.NEW
                            }
                        }
                        PinStage.NEW -> {
                            if (newPin.length != 4) {
                                error = msg(R.string.settings_lock_enter_new)
                            } else {
                                stage = PinStage.CONFIRM
                            }
                        }
                        PinStage.CONFIRM -> {
                            if (newPin != confirmPin) {
                                error = msg(R.string.settings_lock_no_match)
                            } else {
                                onConfirm(newPin)
                            }
                        }
                    }
                },
            ) {
                Text(
                    when (stage) {
                        PinStage.CURRENT ->
                            if (action == PinAction.REMOVE) stringResource(R.string.settings_lock_remove)
                            else stringResource(R.string.confirm)
                        PinStage.NEW -> stringResource(R.string.confirm)
                        PinStage.CONFIRM -> stringResource(R.string.save)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt(),
)
