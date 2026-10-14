package com.alamal.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.alamalDataStore: DataStore<Preferences> by preferencesDataStore(name = "alamal_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_COLOR = stringPreferencesKey("app_color")
        val CURRENCY = stringPreferencesKey("currency")
        val HOURLY_RATE = floatPreferencesKey("hourly_rate")
        val PIN = stringPreferencesKey("pin")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val BACKUP_ENCRYPTED = booleanPreferencesKey("backup_encrypted")
        val BACKUP_TREE_URI = stringPreferencesKey("backup_tree_uri")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val LONG_SHIFT_HOURS = intPreferencesKey("long_shift_hours")
        val LAST_NOTIFIED_DAY_ID = longPreferencesKey("last_notified_day_id")
    }

    val settings: Flow<AppSettings> = context.alamalDataStore.data.map { p ->
        // Auto-migrate from the old blue default to the new gold default.
        // If the user has explicitly picked a different color, we keep it.
        val rawColor = p[Keys.APP_COLOR] ?: AppSettings.DEFAULT_COLOR_HEX
        val colorHex = if (rawColor.equals(
                AppSettings.LEGACY_DEFAULT_COLOR_HEX,
                ignoreCase = true,
            )
        ) AppSettings.DEFAULT_COLOR_HEX else rawColor

        AppSettings(
            themeMode = ThemeMode.fromKey(p[Keys.THEME_MODE]),
            appColorHex = colorHex,
            currency = p[Keys.CURRENCY] ?: "DH",
            hourlyRate = (p[Keys.HOURLY_RATE] ?: 10f).toDouble(),
            pin = p[Keys.PIN] ?: "",
            autoBackupEnabled = p[Keys.AUTO_BACKUP_ENABLED] ?: true,
            backupEncrypted = p[Keys.BACKUP_ENCRYPTED] ?: false,
            backupTreeUri = p[Keys.BACKUP_TREE_URI] ?: "",
            notificationsEnabled = p[Keys.NOTIFICATIONS_ENABLED] ?: false,
            longShiftHours = p[Keys.LONG_SHIFT_HOURS] ?: 12,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.alamalDataStore.edit { it[Keys.THEME_MODE] = mode.key }
    }

    suspend fun setAppColorHex(hex: String) {
        context.alamalDataStore.edit { it[Keys.APP_COLOR] = hex }
    }

    suspend fun setCurrency(currency: String) {
        context.alamalDataStore.edit { it[Keys.CURRENCY] = currency }
    }

    suspend fun setHourlyRate(rate: Double) {
        context.alamalDataStore.edit { it[Keys.HOURLY_RATE] = rate.toFloat() }
    }

    suspend fun setPin(pin: String) {
        context.alamalDataStore.edit { it[Keys.PIN] = pin }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.alamalDataStore.edit { it[Keys.AUTO_BACKUP_ENABLED] = enabled }
    }

    suspend fun setBackupEncrypted(enabled: Boolean) {
        context.alamalDataStore.edit { it[Keys.BACKUP_ENCRYPTED] = enabled }
    }

    suspend fun setBackupTreeUri(uri: String) {
        context.alamalDataStore.edit { it[Keys.BACKUP_TREE_URI] = uri }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.alamalDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setLongShiftHours(hours: Int) {
        context.alamalDataStore.edit { it[Keys.LONG_SHIFT_HOURS] = hours.coerceIn(2, 24) }
    }

    suspend fun getLastNotifiedDayId(): Long {
        return context.alamalDataStore.data.first()[Keys.LAST_NOTIFIED_DAY_ID] ?: 0L
    }

    suspend fun setLastNotifiedDayId(dayId: Long) {
        context.alamalDataStore.edit { it[Keys.LAST_NOTIFIED_DAY_ID] = dayId }
    }
}
