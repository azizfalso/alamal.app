package com.alamal.app.data.prefs

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appColorHex: String = DEFAULT_COLOR_HEX,
    val currency: String = "DH",
    val hourlyRate: Double = 10.0,
    val pin: String = "",
    val autoBackupEnabled: Boolean = true,
    val backupEncrypted: Boolean = false,
    val backupTreeUri: String = "",
    val notificationsEnabled: Boolean = false,
    val longShiftHours: Int = 12,
) {
    companion object {
        const val DEFAULT_COLOR_HEX = "#4D6BFE"
        val DEFAULT = AppSettings()
    }
}
