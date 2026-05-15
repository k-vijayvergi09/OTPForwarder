package com.samsung.android.otpforwarder.feature.settings

// ── State ─────────────────────────────────────────────────────────────────────

/**
 * UI state for the Settings screen.
 *
 * As of the Rules → Destinations migration, the Settings screen no longer manages
 * phone/email destinations — those live in `:feature:destinations`. Settings is
 * now strictly for cross-cutting preferences (master toggle, delay, notifications,
 * developer mode).
 */
data class SettingsState(
    val isForwardingEnabled: Boolean = true,
    val forwardingDelaySeconds: Int = 0,
    val notificationsEnabled: Boolean = true,
    val isDeveloperModeEnabled: Boolean = false,
    val isLoading: Boolean = true,
)

// ── Intents ───────────────────────────────────────────────────────────────────

sealed interface SettingsIntent {
    data object ToggleForwarding                     : SettingsIntent
    data class  SetForwardingDelay(val seconds: Int) : SettingsIntent
    data object ToggleNotifications                  : SettingsIntent
    data object ToggleDeveloperMode                  : SettingsIntent
    data object NavigateBack                         : SettingsIntent
}

// ── Side effects ──────────────────────────────────────────────────────────────

sealed interface SettingsSideEffect {
    data object GoBack                            : SettingsSideEffect
    data class  ShowSnackbar(val message: String) : SettingsSideEffect
}
