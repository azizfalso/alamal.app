package com.alamal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.lifecycleScope
import com.alamal.app.data.prefs.AppSettings
import com.alamal.app.data.prefs.SettingsRepository
import com.alamal.app.ui.navigation.AppNavigation
import com.alamal.app.ui.screens.lock.LockScreen
import com.alamal.app.ui.theme.AlamalTheme
import com.alamal.app.ui.theme.parseHexColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepo: SettingsRepository

    private val isLocked = mutableStateOf<Boolean?>(null)
    private var backgroundedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settingsRepo = SettingsRepository(applicationContext)

        lifecycleScope.launch {
            val s = settingsRepo.settings.first()
            isLocked.value = s.pin.isNotEmpty()
        }

        setContent {
            val settings by settingsRepo.settings.collectAsState(initial = AppSettings.DEFAULT)
            val locked = isLocked.value

            LaunchedEffect(settings.pin) {
                if (settings.pin.isEmpty() && isLocked.value == true) {
                    isLocked.value = false
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AlamalTheme(
                    themeMode = settings.themeMode,
                    seedColor = parseHexColor(settings.appColorHex),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        when {
                            locked == null -> Unit
                            locked && settings.pin.isNotEmpty() -> {
                                LockScreen(
                                    expectedPin = settings.pin,
                                    onUnlock = { isLocked.value = false },
                                )
                            }
                            else -> AppNavigation()
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundedAt = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (backgroundedAt > 0L) {
            val elapsed = System.currentTimeMillis() - backgroundedAt
            backgroundedAt = 0L
            if (elapsed > LOCK_AFTER_MS) {
                lifecycleScope.launch {
                    val s = settingsRepo.settings.first()
                    if (s.pin.isNotEmpty()) {
                        isLocked.value = true
                    }
                }
            }
        }
    }

    private companion object {
        const val LOCK_AFTER_MS = 30_000L
    }
}
