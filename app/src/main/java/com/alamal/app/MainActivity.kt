package com.alamal.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.alamal.app.data.prefs.AppSettings
import com.alamal.app.data.prefs.SettingsRepository
import com.alamal.app.lock.AppLockController
import com.alamal.app.ui.navigation.AppNavigation
import com.alamal.app.ui.screens.lock.LockScreen
import com.alamal.app.ui.theme.AlamalTheme
import com.alamal.app.ui.theme.parseHexColor

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepo: SettingsRepository
    private val pendingShortcutAction = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settingsRepo = SettingsRepository(applicationContext)

        handleShortcutIntent(intent)

        // On cold start, if locking is enabled, start locked.
        if (AppLockController.isEnabled()) {
            AppLockController.lock()
        }

        setContent {
            val settings by settingsRepo.settings.collectAsState(initial = AppSettings.DEFAULT)
            val isLocked by AppLockController.isLocked.collectAsState()

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AlamalTheme(
                    themeMode = settings.themeMode,
                    seedColor = parseHexColor(settings.appColorHex),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        if (isLocked && settings.pin.isNotEmpty()) {
                            LockScreen(
                                expectedPin = settings.pin,
                                onUnlock = { AppLockController.unlock() },
                            )
                        } else {
                            AppNavigation(
                                pendingShortcutAction = pendingShortcutAction,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
    }

    private fun handleShortcutIntent(intent: Intent?) {
        val action = intent?.getStringExtra(EXTRA_SHORTCUT_ACTION) ?: return
        if (action == SHORTCUT_CLOCK_IN || action == SHORTCUT_CLOCK_OUT) {
            pendingShortcutAction.value = action
        }
    }

    private companion object {
        const val EXTRA_SHORTCUT_ACTION = "shortcut_action"
        const val SHORTCUT_CLOCK_IN = "clock_in"
        const val SHORTCUT_CLOCK_OUT = "clock_out"
    }
}
