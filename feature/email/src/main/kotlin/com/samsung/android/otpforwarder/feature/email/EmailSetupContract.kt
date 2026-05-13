package com.samsung.android.otpforwarder.feature.email

// ── State ─────────────────────────────────────────────────────────────────────

data class EmailSetupState(
    /** The Gmail address currently typed in the field. */
    val address: String = "",
    /** The App Password currently typed in the field. */
    val appPassword: String = "",
    /** Whether the app password field is visible as plain text. */
    val isPasswordVisible: Boolean = false,
    /** True while a test-connection or save is in progress. */
    val isLoading: Boolean = false,
    /** True if credentials are already saved (controls "Clear" button visibility). */
    val isConfigured: Boolean = false,
    /** Non-null when a connection test has completed — shown as a status banner. */
    val testResult: TestResult? = null,
    /** True when the "What's an App Password?" info bottom sheet is open. */
    val showAppPasswordInfo: Boolean = false,
)

sealed interface TestResult {
    data object Success : TestResult
    data class Failure(val reason: String) : TestResult
}

// ── Intents ───────────────────────────────────────────────────────────────────

sealed interface EmailSetupIntent {
    data class UpdateAddress(val value: String)     : EmailSetupIntent
    data class UpdateAppPassword(val value: String) : EmailSetupIntent
    data object TogglePasswordVisibility             : EmailSetupIntent
    data object TestAndSave                          : EmailSetupIntent
    data object ClearCredentials                     : EmailSetupIntent
    data object NavigateBack                         : EmailSetupIntent
    data object DismissTestResult                    : EmailSetupIntent
    data object ShowAppPasswordInfo                  : EmailSetupIntent
    data object DismissAppPasswordInfo               : EmailSetupIntent
}

// ── Side effects ──────────────────────────────────────────────────────────────

sealed interface EmailSetupSideEffect {
    data object GoBack                           : EmailSetupSideEffect
    data class  ShowSnackbar(val message: String): EmailSetupSideEffect
}
