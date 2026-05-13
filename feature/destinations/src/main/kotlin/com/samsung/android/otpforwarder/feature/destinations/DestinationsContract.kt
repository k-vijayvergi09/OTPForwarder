package com.samsung.android.otpforwarder.feature.destinations

// ── UI models ─────────────────────────────────────────────────────────────────

data class SmsDestinationUiItem(
    val id: String,
    val label: String,
    val phoneNumber: String,
    val isEnabled: Boolean,
)

data class EmailDestinationUiItem(
    val id: String,
    val label: String,
    val emailAddress: String,
    val isEnabled: Boolean,
)

// ── MVI contract ──────────────────────────────────────────────────────────────

data class DestinationsState(
    val smsDestinations: List<SmsDestinationUiItem> = emptyList(),
    val emailDestinations: List<EmailDestinationUiItem> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface DestinationsIntent {
    data object AddPhone                                    : DestinationsIntent
    data object AddEmail                                    : DestinationsIntent
    data class  ToggleSms(val id: String)                  : DestinationsIntent
    data class  ToggleEmail(val id: String)                 : DestinationsIntent
    data class  DeleteSms(val id: String)                   : DestinationsIntent
    data class  DeleteEmail(val id: String)                 : DestinationsIntent
    data object NavigateToSettings                          : DestinationsIntent
}

sealed interface DestinationsSideEffect {
    data object NavigateToAddPhone     : DestinationsSideEffect
    data object NavigateToAddEmail     : DestinationsSideEffect
    data object GoToSettings           : DestinationsSideEffect
}
