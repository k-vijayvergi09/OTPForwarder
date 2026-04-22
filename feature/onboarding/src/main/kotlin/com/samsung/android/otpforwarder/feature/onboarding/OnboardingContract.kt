package com.samsung.android.otpforwarder.feature.onboarding

// ── Steps ─────────────────────────────────────────────────────────────────────

enum class OnboardingStep {
    WELCOME,
    SMS_PERMISSION,
    NOTIFICATION_PERMISSION,
    BATTERY_OPTIMIZATION,
    DONE,
}

// ── State ─────────────────────────────────────────────────────────────────────

data class OnboardingState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val smsDenied: Boolean = false,
    val notificationDenied: Boolean = false,
)

val OnboardingStep.index: Int get() = ordinal
val OnboardingStep.isLast: Boolean get() = this == OnboardingStep.DONE

// ── Intents ───────────────────────────────────────────────────────────────────

sealed interface OnboardingIntent {
    data object Next                               : OnboardingIntent
    data object Back                               : OnboardingIntent
    data object RequestSmsPermission               : OnboardingIntent
    data object RequestNotificationPermission      : OnboardingIntent
    data object RequestBatteryOptExemption         : OnboardingIntent
    data class  SmsDenied(val permanently: Boolean): OnboardingIntent
    data object SmsGranted                         : OnboardingIntent
    data object NotificationGranted                : OnboardingIntent
    data object NotificationDenied                 : OnboardingIntent
    data object Finish                             : OnboardingIntent
}

// ── Side effects ──────────────────────────────────────────────────────────────

sealed interface OnboardingSideEffect {
    data object LaunchSmsPermission          : OnboardingSideEffect
    data object LaunchNotificationPermission : OnboardingSideEffect
    data object LaunchBatteryOptSettings     : OnboardingSideEffect
    data object NavigateToHome               : OnboardingSideEffect
}
