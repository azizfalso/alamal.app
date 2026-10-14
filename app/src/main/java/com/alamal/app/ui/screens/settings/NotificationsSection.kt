package com.alamal.app.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.alamal.app.data.prefs.AppSettings
import com.alamal.app.data.prefs.SettingsRepository
import com.alamal.app.ui.components.AlamalCard
import com.alamal.app.ui.components.AlamalDivider
import com.alamal.app.ui.theme.LocalExtendedColors
import com.alamal.app.work.NotificationScheduler
import kotlinx.coroutines.launch

@Composable
fun NotificationsSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepo = remember { SettingsRepository(context) }
    val settings by settingsRepo.settings.collectAsState(initial = AppSettings.DEFAULT)

    var hoursText by remember(settings.longShiftHours) {
        mutableStateOf(settings.longShiftHours.toString())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            scope.launch { settingsRepo.setNotificationsEnabled(true) }
            NotificationScheduler.schedule(context)
        } else {
            // Keep toggle off if permission denied.
            scope.launch { settingsRepo.setNotificationsEnabled(false) }
            NotificationScheduler.cancel(context)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    scope.launch { settingsRepo.setNotificationsEnabled(true) }
                    NotificationScheduler.schedule(context)
                } else {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                scope.launch { settingsRepo.setNotificationsEnabled(true) }
                NotificationScheduler.schedule(context)
            }
        } else {
            scope.launch { settingsRepo.setNotificationsEnabled(false) }
            NotificationScheduler.cancel(context)
        }
    }

    AlamalCard {
        Text(
            text = "الإشعارات",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = LocalExtendedColors.current.accent,
        )
        AlamalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { toggleNotifications(!settings.notificationsEnabled) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(
                checked = settings.notificationsEnabled,
                onCheckedChange = ::toggleNotifications,
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = "تنبيه عند ساعات العمل الطويلة",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "يُنبّهك عند نسيان تسجيل الخروج من العمل",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalExtendedColors.current.muted,
                )
            }
        }

        if (settings.notificationsEnabled) {
            AlamalDivider()
            Text(
                text = "عدد الساعات قبل التنبيه",
                style = MaterialTheme.typography.labelLarge,
                color = LocalExtendedColors.current.muted,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = hoursText,
                onValueChange = { newVal ->
                    hoursText = newVal.filter(Char::isDigit).take(2)
                    val parsed = hoursText.toIntOrNull()
                    if (parsed != null && parsed in 2..24) {
                        scope.launch { settingsRepo.setLongShiftHours(parsed) }
                    }
                },
                label = { Text("الساعات (2 - 24)") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "الفحص يحدث كل 30 دقيقة تقريبًا ولا يستهلك البطارية بشكل ملحوظ.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalExtendedColors.current.meta,
            )
        }
    }
}
