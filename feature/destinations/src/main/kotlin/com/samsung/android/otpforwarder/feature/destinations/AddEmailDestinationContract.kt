package com.samsung.android.otpforwarder.feature.destinations

// ── State ─────────────────────────────────────────────────────────────────────

data class AddEmailState(
    val label: String = "",
    val emailAddress: String = "",
    val enableImmediately: Boolean = true,
    val isSaving: Boolean = false,
    val emailError: String? = null,
)

// ── Intents ───────────────────────────────────────────────────────────────────

sealed interface AddEmailIntent {
    data class LabelChanged(val value: String)       : AddEmailIntent
    data class AddressChanged(val value: String)     : AddEmailIntent
    data object ToggleEnable                         : AddEmailIntent
    data object Save                                 : AddEmailIntent
    data object NavigateBack                         : AddEmailIntent
}

// ── Side effects ──────────────────────────────────────────────────────────────

sealed interface AddEmailSideEffect {
    data object GoBack : AddEmailSideEffect
}
