package com.alamal.app.data.prefs

enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(k: String?): ThemeMode = when (k) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> SYSTEM
        }
    }
}
