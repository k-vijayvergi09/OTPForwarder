package com.samsung.android.otpforwarder.feature.settings

import androidx.lifecycle.ViewModel
import com.samsung.android.otpforwarder.core.domain.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel(), ContainerHost<SettingsState, SettingsSideEffect> {

    override val container = container<SettingsState, SettingsSideEffect>(SettingsState()) {
        // Observe persisted settings and keep UI state in sync.
        intent {
            settingsRepository.settings.collectLatest { appSettings ->
                reduce {
                    state.copy(
                        isForwardingEnabled    = appSettings.isForwardingEnabled,
                        forwardingDelaySeconds = appSettings.forwardingDelaySeconds,
                        notificationsEnabled   = appSettings.notificationsEnabled,
                        isDeveloperModeEnabled = appSettings.isDeveloperModeEnabled,
                        isLoading              = false,
                    )
                }
            }
        }
    }

    fun onIntent(intent: SettingsIntent) = when (intent) {

        SettingsIntent.ToggleForwarding -> intent {
            settingsRepository.updateSettings { it.copy(isForwardingEnabled = !it.isForwardingEnabled) }
        }

        is SettingsIntent.SetForwardingDelay -> intent {
            settingsRepository.updateSettings {
                it.copy(forwardingDelaySeconds = intent.seconds.coerceIn(0, 30))
            }
        }

        SettingsIntent.ToggleNotifications -> intent {
            settingsRepository.updateSettings { it.copy(notificationsEnabled = !it.notificationsEnabled) }
        }

        SettingsIntent.ToggleDeveloperMode -> intent {
            settingsRepository.updateSettings { it.copy(isDeveloperModeEnabled = !it.isDeveloperModeEnabled) }
        }

        SettingsIntent.NavigateBack -> intent {
            postSideEffect(SettingsSideEffect.GoBack)
        }
    }
}
