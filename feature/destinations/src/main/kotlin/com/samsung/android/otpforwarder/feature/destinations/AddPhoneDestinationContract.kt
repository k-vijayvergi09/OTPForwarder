package com.samsung.android.otpforwarder.feature.destinations

// ── State ─────────────────────────────────────────────────────────────────────

data class AddPhoneState(
    val label: String = "",
    val phoneNumber: String = "",
    val enableImmediately: Boolean = true,
    val isSaving: Boolean = false,
    val phoneError: String? = null,
)

// ── Intents ───────────────────────────────────────────────────────────────────

sealed interface AddPhoneIntent {
    data class LabelChanged(val value: String)       : AddPhoneIntent
    data class NumberChanged(val value: String)      : AddPhoneIntent
    data object ToggleEnable                         : AddPhoneIntent
    data object Save                                 : AddPhoneIntent
    data object NavigateBack                         : AddPhoneIntent
}

// ── Side effects ──────────────────────────────────────────────────────────────

sealed interface AddPhoneSideEffect {
    data object GoBack : AddPhoneSideEffect
}
