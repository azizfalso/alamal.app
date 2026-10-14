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

    /** Call when the PIN setting changes (set, changed, removed). */
    fun configure(pin: String) {
        expectedPin = pin
        lockingEnabled = pin.isNotEmpty()
        if (!lockingEnabled) {
            _isLocked.value = false
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
