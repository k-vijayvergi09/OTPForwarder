package com.samsung.android.otpforwarder.core.model

/**
 * Persisted app-wide preferences (stored in EncryptedDataStore in M5;
 * in-memory for M1–M4).
 *
 * Destinations are no longer a global single-value field — they live in their
 * own Room-backed tables (see [SmsDestination] / [EmailDestination] +
 * `SmsDestinationRepository` / `EmailDestinationRepository`). AppSettings is
 * now strictly for *cross-cutting* preferences that don't belong to any one
 * destination.
 *
 * @param isForwardingEnabled    Master switch — when false no SMS is forwarded.
 * @param forwardingDelaySeconds Delay before forwarding (0 = immediate, max 30s).
 * @param isBiometricLockEnabled Whether the app requires biometric / PIN to open.
 * @param notificationsEnabled   Whether to show a notification on each forwarding event.
 * @param isFirstLaunch          Cleared after onboarding completes.
 */
data class AppSettings(
    val isForwardingEnabled: Boolean = true,
    val forwardingDelaySeconds: Int = 0,
    val isBiometricLockEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val isFirstLaunch: Boolean = true,
)
