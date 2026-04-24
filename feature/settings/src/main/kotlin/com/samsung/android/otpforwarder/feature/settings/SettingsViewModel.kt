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
                        defaultDestinations    = appSettings.defaultDestinations.toSet(),
                        defaultPhoneNumber     = appSettings.defaultPhoneNumber,
                        defaultEmailAddress    = appSettings.defaultEmailAddress,
                        isBiometricLockEnabled = appSettings.isBiometricLockEnabled,
                        notificationsEnabled   = appSettings.notificationsEnabled,
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

        is SettingsIntent.ToggleDestination -> intent {
            settingsRepository.updateSettings { settings ->
                val current = settings.defaultDestinations.toMutableList()
                if (intent.type in current) {
                    if (current.size > 1) current.remove(intent.type)
                } else {
                    current.add(intent.type)
                }
                settings.copy(defaultDestinations = current)
            }
        }

        SettingsIntent.ToggleBiometricLock -> intent {
            settingsRepository.updateSettings { it.copy(isBiometricLockEnabled = !it.isBiometricLockEnabled) }
        }

        SettingsIntent.ToggleNotifications -> intent {
            settingsRepository.updateSettings { it.copy(notificationsEnabled = !it.notificationsEnabled) }
        }

        // ── Destination dialogs ───────────────────────────────────────────────

        SettingsIntent.ShowPhoneNumberDialog -> intent {
            reduce { state.copy(showPhoneNumberDialog = true) }
        }

        SettingsIntent.HidePhoneNumberDialog -> intent {
            reduce { state.copy(showPhoneNumberDialog = false) }
        }

        is SettingsIntent.SavePhoneNumber -> intent {
            settingsRepository.updateSettings { it.copy(defaultPhoneNumber = intent.number.trim()) }
            reduce { state.copy(showPhoneNumberDialog = false) }
            postSideEffect(SettingsSideEffect.ShowSnackbar("SMS destination saved"))
        }

        SettingsIntent.ShowEmailDialog -> intent {
            reduce { state.copy(showEmailDialog = true) }
        }

        SettingsIntent.HideEmailDialog -> intent {
            reduce { state.copy(showEmailDialog = false) }
        }

        is SettingsIntent.SaveEmail -> intent {
            settingsRepository.updateSettings { it.copy(defaultEmailAddress = intent.email.trim()) }
            reduce { state.copy(showEmailDialog = false) }
            postSideEffect(SettingsSideEffect.ShowSnackbar("Email destination saved"))
        }

        // ── Data management ───────────────────────────────────────────────────

        SettingsIntent.ExportConfig -> intent {
            postSideEffect(SettingsSideEffect.LaunchExportFilePicker)
        }

        SettingsIntent.ImportConfig -> intent {
            postSideEffect(SettingsSideEffect.LaunchImportFilePicker)
        }

        // ── Navigation ────────────────────────────────────────────────────────

        SettingsIntent.NavigateBack -> intent {
            postSideEffect(SettingsSideEffect.GoBack)
        }
    }
}
