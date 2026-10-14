package com.alamal.app.lock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives ACTION_SCREEN_OFF (and USER_PRESENT) dynamically.
 *
 * ACTION_SCREEN_OFF cannot be registered in the manifest on Android 8+,
 * so we register this receiver at runtime from AlamalApp.
 */
class ScreenLockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SCREEN_OFF -> {
                // Lock immediately when the user turns off the screen.
                AppLockController.lock()
            }
        }
    }
}
