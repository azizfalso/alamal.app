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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.alamal.app.ui.theme.ThemeColorPresets
import com.alamal.app.ui.theme.isHexColorValid
import com.alamal.app.ui.theme.parseHexColor
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsState(initial = AppSettings.DEFAULT)

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeColorPresets.take(5).forEach { preset ->
                    ColorDot(
                        color = parseHexColor(preset.hex),
                        selected = settings.appColorHex.equals(preset.hex, ignoreCase = true),
                        onClick = { scope.launch { repo.setAppColorHex(preset.hex) } },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeColorPresets.drop(5).forEach { preset ->
                    ColorDot(
                        color = parseHexColor(preset.hex),
                        selected = settings.appColorHex.equals(preset.hex, ignoreCase = true),
                        onClick = { scope.launch { repo.setAppColorHex(preset.hex) } },
                    )
                }
            }

            var customHex by remember { mutableStateOf("") }
            var customError by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = customHex,
                    onValueChange = {
                        customHex = it
                        customError = false
                    },
                    label = { Text(stringResource(R.string.settings_color_custom)) },
                    placeholder = { Text(stringResource(R.string.settings_color_custom_hint)) },
                    singleLine = true,
                    isError = customError,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        if (isHexColorValid(customHex)) {
                            scope.launch { repo.setAppColorHex(customHex.trim()) }
                            customHex = ""
                        } else {
                            customError = true
                        }
                    },
                ) {
                    Text(stringResource(R.string.settings_color_apply))
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
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun ColorDot(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            )
            .clickable { onClick() }
    )
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
