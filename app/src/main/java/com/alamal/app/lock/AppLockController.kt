package com.alamal.app.lock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central lock state for the whole app.
 *
 * - Holds the current locked/unlocked state as a StateFlow.
 * - Holds the expected PIN and whether locking is enabled.
 * - Shared between MainActivity, Application observers, and services.
 */
object AppLockController {

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    @Volatile
    private var expectedPin: String = ""

    @Volatile
    private var lockingEnabled: Boolean = false

    // Tracks whether the "cold-start lock" has already been applied in this
    // process. Lives only in memory — resets on process death, which is
    // exactly what we want: the next cold start should lock again.
    @Volatile
    private var hasAppliedInitialLock: Boolean = false

    /** Call when the PIN setting changes (set, changed, removed). */
    fun configure(pin: String) {
        expectedPin = pin
        lockingEnabled = pin.isNotEmpty()
        if (!lockingEnabled) {
            _isLocked.value = false
        }
    }

    /**
     * Applies the lock exactly once per process, the first time we learn
     * that a PIN is configured.
     *
     * Why this exists:
     *   MainActivity.onCreate used to call lock() directly, but on a cold
     *   start SettingsRepository.settings had not emitted yet, so
     *   isEnabled() returned false and the lock was skipped — the user
     *   landed on the home screen without any authentication.
     *
     *   Now MainActivity waits for the first emission and calls this method.
     *   The `hasAppliedInitialLock` flag makes it idempotent: later PIN
     *   changes within the same process don't re-trigger the lock (the user
     *   is already inside the app).
     */
    fun applyInitialLockIfNeeded(pin: String) {
        if (hasAppliedInitialLock) return
        hasAppliedInitialLock = true
        if (pin.isNotEmpty()) {
            _isLocked.value = true
        }
    }

    /** Force a lock, if locking is enabled. Safe to call from any thread. */
    fun lock() {
        if (lockingEnabled) _isLocked.value = true
    }

    /** Mark the app as unlocked. */
    fun unlock() {
        _isLocked.value = false
    }

    fun isEnabled(): Boolean = lockingEnabled

    fun expectedPin(): String = expectedPin
}
