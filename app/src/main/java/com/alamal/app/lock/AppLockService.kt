package com.alamal.app.lock

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder

/**
 * Lightweight started Service whose only purpose is to react to
 * removal of the app from the Recents screen via onTaskRemoved().
 *
 * Notes:
 *  - Do NOT set android:stopWithTask="true" on this service.
 *  - No foreground notification is used; this is best-effort and may
 *    not fire on some OEM ROMs, but it does no harm when it does.
 *  - The service also dynamically registers ScreenLockReceiver while
 *    running, which is the recommended way to catch ACTION_SCREEN_OFF.
 */
class AppLockService : Service() {

    private val screenReceiver = ScreenLockReceiver()
    private var registered = false

    override fun onCreate() {
        super.onCreate()
        registerScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY so the system tries to keep us alive briefly.
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // App was swiped away from Recents → lock immediately.
        AppLockController.lock()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        unregisterScreenReceiver()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerScreenReceiver() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(screenReceiver, filter)
        }
        registered = true
    }

    private fun unregisterScreenReceiver() {
        if (!registered) return
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Throwable) {
            // Ignore — receiver may already be gone.
        }
        registered = false
    }
}
