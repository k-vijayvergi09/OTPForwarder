package com.samsung.android.otpforwarder.feature.onboarding

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor() :
    ViewModel(), ContainerHost<OnboardingState, OnboardingSideEffect> {

    override val container = container<OnboardingState, OnboardingSideEffect>(OnboardingState())

    fun onIntent(intent: OnboardingIntent) = when (intent) {
        OnboardingIntent.Next -> intent {
            val next = OnboardingStep.entries.getOrNull(state.currentStep.index + 1)
                ?: return@intent
            reduce { state.copy(currentStep = next) }
        }

        OnboardingIntent.Back -> intent {
            val prev = OnboardingStep.entries.getOrNull(state.currentStep.index - 1)
                ?: return@intent
            reduce { state.copy(currentStep = prev) }
        }

        OnboardingIntent.RequestSmsPermission -> intent {
            postSideEffect(OnboardingSideEffect.LaunchSmsPermission)
        }

        OnboardingIntent.SmsGranted -> intent {
            reduce { state.copy(smsDenied = false, currentStep = OnboardingStep.NOTIFICATION_PERMISSION) }
        }

        is OnboardingIntent.SmsDenied -> intent {
            reduce { state.copy(smsDenied = true) }
        }

        OnboardingIntent.RequestNotificationPermission -> intent {
            postSideEffect(OnboardingSideEffect.LaunchNotificationPermission)
        }

        OnboardingIntent.NotificationGranted -> intent {
            reduce { state.copy(notificationDenied = false, currentStep = OnboardingStep.BATTERY_OPTIMIZATION) }
        }

        OnboardingIntent.NotificationDenied -> intent {
            // Non-fatal — user can still proceed without notification permission
            reduce { state.copy(notificationDenied = true, currentStep = OnboardingStep.BATTERY_OPTIMIZATION) }
        }

        OnboardingIntent.RequestBatteryOptExemption -> intent {
            postSideEffect(OnboardingSideEffect.LaunchBatteryOptSettings)
            // After returning from settings we auto-advance; user can also tap Skip
            reduce { state.copy(currentStep = OnboardingStep.DONE) }
        }

        OnboardingIntent.Finish -> intent {
            postSideEffect(OnboardingSideEffect.NavigateToHome)
        }
    }
}
