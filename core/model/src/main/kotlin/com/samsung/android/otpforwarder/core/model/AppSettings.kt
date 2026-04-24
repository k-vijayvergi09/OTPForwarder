package com.samsung.android.otpforwarder.core.model

/**
 * Persisted app-wide preferences (stored in EncryptedDataStore).
 *
 * @param isForwardingEnabled    Master switch — when false no SMS is forwarded regardless of rules.
 * @param forwardingDelaySeconds Delay before forwarding (0 = immediate). Max 30s.
 * @param defaultDestinations    Destinations used when no matching rule exists.
 * @param isBiometricLockEnabled Whether the app requires biometric / PIN to open.
 * @param notificationsEnabled   Whether to show a notification on each forwarding event.
 * @param isFirstLaunch          Cleared after onboarding completes.
 */
data class AppSettings(
    val isForwardingEnabled: Boolean = true,
    val forwardingDelaySeconds: Int = 0,
    val defaultDestinations: List<DestinationType> = listOf(DestinationType.EMAIL),
    val defaultPhoneNumber: String = "",
    val defaultEmailAddress: String = "",
    val isBiometricLockEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val isFirstLaunch: Boolean = true,
)
