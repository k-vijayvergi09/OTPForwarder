package com.samsung.android.otpforwarder.core.model

/**
 * Persisted app-wide preferences (stored in EncryptedDataStore in M5;
 * in-memory for M1–M4).
 *
 * @param isForwardingEnabled    Master switch — when false no SMS is forwarded.
 * @param forwardingDelaySeconds Delay before forwarding (0 = immediate, max 30s).
 * @param defaultDestinations    Channels used when no specific rule matches.
 * @param defaultPhoneNumber     Phone number for SMS forwarding (E.164 recommended).
 * @param defaultEmailAddress    Email address for email forwarding.
 * @param isBiometricLockEnabled Whether the app requires biometric / PIN to open.
 * @param notificationsEnabled   Whether to show a notification on each forwarding event.
 * @param isFirstLaunch          Cleared after onboarding completes.
 */
data class AppSettings(
    val isForwardingEnabled: Boolean = true,
    val forwardingDelaySeconds: Int = 0,
    val defaultDestinations: List<DestinationType> = listOf(DestinationType.SMS),
    val defaultPhoneNumber: String = "",
    val defaultEmailAddress: String = "",
    val isBiometricLockEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val isFirstLaunch: Boolean = true,
)
