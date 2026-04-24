package com.samsung.android.otpforwarder.feature.settings

import androidx.lifecycle.ViewModel
import com.samsung.android.otpforwarder.core.model.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    // SettingsRepository will be injected here in M5 when EncryptedDataStore is wired up.
    // For now the ViewModel manages in-memory state seeded from AppSettings defaults.
) : ViewModel(), ContainerHost<SettingsState, SettingsSideEffect> {

    private val defaults = AppSettings()

    override val container = container<SettingsState, SettingsSideEffect>(
        SettingsState(
            isForwardingEnabled    = defaults.isForwardingEnabled,
            forwardingDelaySeconds = defaults.forwardingDelaySeconds,
            defaultDestinations    = defaults.defaultDestinations.toSet(),
            defaultPhoneNumber     = defaults.defaultPhoneNumber,
            defaultEmailAddress    = defaults.defaultEmailAddress,
            showPhoneNumberDialog  = false,
            showEmailDialog        = false,
            isBiometricLockEnabled = defaults.isBiometricLockEnabled,
            notificationsEnabled   = defaults.notificationsEnabled,
            isLoading              = false,
        )
    )

    fun onIntent(intent: SettingsIntent) = when (intent) {
        SettingsIntent.ToggleForwarding -> intent {
            reduce { state.copy(isForwardingEnabled = !state.isForwardingEnabled) }
        }

        is SettingsIntent.SetForwardingDelay -> intent {
            reduce { state.copy(forwardingDelaySeconds = intent.seconds.coerceIn(0, 30)) }
        }

        is SettingsIntent.ToggleDestination -> intent {
            reduce {
                val current = state.defaultDestinations.toMutableSet()
                if (intent.type in current) {
                    // Always keep at least one destination selected
                    if (current.size > 1) current.remove(intent.type)
                } else {
                    current.add(intent.type)
                }
                state.copy(defaultDestinations = current)
            }
        }

        SettingsIntent.ToggleBiometricLock -> intent {
            reduce { state.copy(isBiometricLockEnabled = !state.isBiometricLockEnabled) }
        }

        SettingsIntent.ToggleNotifications -> intent {
            reduce { state.copy(notificationsEnabled = !state.notificationsEnabled) }
        }

        SettingsIntent.ShowPhoneNumberDialog -> intent {
            reduce { state.copy(showPhoneNumberDialog = true) }
        }

        SettingsIntent.HidePhoneNumberDialog -> intent {
            reduce { state.copy(showPhoneNumberDialog = false) }
        }

        is SettingsIntent.SavePhoneNumber -> intent {
            reduce { state.copy(defaultPhoneNumber = intent.number, showPhoneNumberDialog = false) }
        }

        SettingsIntent.ShowEmailDialog -> intent {
            reduce { state.copy(showEmailDialog = true) }
        }

        SettingsIntent.HideEmailDialog -> intent {
            reduce { state.copy(showEmailDialog = false) }
        }

        is SettingsIntent.SaveEmail -> intent {
            reduce { state.copy(defaultEmailAddress = intent.email, showEmailDialog = false) }
        }

        SettingsIntent.ExportConfig -> intent {
            postSideEffect(SettingsSideEffect.LaunchExportFilePicker)
        }

        SettingsIntent.ImportConfig -> intent {
            postSideEffect(SettingsSideEffect.LaunchImportFilePicker)
        }

        SettingsIntent.NavigateBack -> intent {
            postSideEffect(SettingsSideEffect.GoBack)
        }
    }
}
