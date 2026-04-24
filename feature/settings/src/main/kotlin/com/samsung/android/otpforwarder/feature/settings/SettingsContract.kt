package com.samsung.android.otpforwarder.feature.settings

import com.samsung.android.otpforwarder.core.model.DestinationType

// ── State ─────────────────────────────────────────────────────────────────────

data class SettingsState(
    val isForwardingEnabled: Boolean = true,
    val forwardingDelaySeconds: Int = 0,
    val defaultDestinations: Set<DestinationType> = setOf(DestinationType.EMAIL),
    val defaultPhoneNumber: String = "",
    val defaultEmailAddress: String = "",
    val showPhoneNumberDialog: Boolean = false,
    val showEmailDialog: Boolean = false,
    val isBiometricLockEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val isLoading: Boolean = true,
)

// ── Intents ───────────────────────────────────────────────────────────────────

sealed interface SettingsIntent {
    data object ToggleForwarding                             : SettingsIntent
    data class  SetForwardingDelay(val seconds: Int)         : SettingsIntent
    data class  ToggleDestination(val type: DestinationType) : SettingsIntent
    data object ToggleBiometricLock                          : SettingsIntent
    data object ToggleNotifications                          : SettingsIntent
    data object ShowPhoneNumberDialog                        : SettingsIntent
    data object HidePhoneNumberDialog                        : SettingsIntent
    data class  SavePhoneNumber(val number: String)          : SettingsIntent
    data object ShowEmailDialog                              : SettingsIntent
    data object HideEmailDialog                              : SettingsIntent
    data class  SaveEmail(val email: String)                 : SettingsIntent
    data object ExportConfig                                 : SettingsIntent
    data object ImportConfig                                 : SettingsIntent
    data object NavigateBack                                 : SettingsIntent
}

// ── Side effects ──────────────────────────────────────────────────────────────

sealed interface SettingsSideEffect {
    data object GoBack                      : SettingsSideEffect
    data object LaunchExportFilePicker      : SettingsSideEffect
    data object LaunchImportFilePicker      : SettingsSideEffect
    data class  ShowSnackbar(val message: String) : SettingsSideEffect
}
