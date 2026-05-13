package com.samsung.android.otpforwarder.feature.settings

// ── State ─────────────────────────────────────────────────────────────────────

/**
 * UI state for the Settings screen.
 *
 * As of the Rules → Destinations migration, the Settings screen no longer manages
 * phone/email destinations — those live in `:feature:destinations`. Settings is
 * now strictly for cross-cutting preferences (master toggle, delay, security,
 * notifications, config export/import).
 */
data class SettingsState(
    val isForwardingEnabled: Boolean = true,
    val forwardingDelaySeconds: Int = 0,
    val isBiometricLockEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val isLoading: Boolean = true,
)

// ── Intents ───────────────────────────────────────────────────────────────────

sealed interface SettingsIntent {
    data object ToggleForwarding                     : SettingsIntent
    data class  SetForwardingDelay(val seconds: Int) : SettingsIntent
    data object ToggleBiometricLock                  : SettingsIntent
    data object ToggleNotifications                  : SettingsIntent
    data object ExportConfig                         : SettingsIntent
    data object ImportConfig                         : SettingsIntent
    data object ConfigureGmail                       : SettingsIntent
    data object NavigateBack                         : SettingsIntent
}

// ── Side effects ──────────────────────────────────────────────────────────────

sealed interface SettingsSideEffect {
    data object GoBack                           : SettingsSideEffect
    data object NavigateToGmailSetup             : SettingsSideEffect
    data object LaunchExportFilePicker           : SettingsSideEffect
    data object LaunchImportFilePicker           : SettingsSideEffect
    data class  ShowSnackbar(val message: String): SettingsSideEffect
}
