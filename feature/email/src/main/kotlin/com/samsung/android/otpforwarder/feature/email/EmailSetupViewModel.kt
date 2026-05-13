package com.samsung.android.otpforwarder.feature.email

import androidx.lifecycle.ViewModel
import com.samsung.android.otpforwarder.core.domain.EmailCredentialRepository
import com.samsung.android.otpforwarder.core.network.EmailSendResult
import com.samsung.android.otpforwarder.core.network.EmailSender
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class EmailSetupViewModel @Inject constructor(
    private val credentialRepository: EmailCredentialRepository,
    private val emailSender: EmailSender,
) : ViewModel(), ContainerHost<EmailSetupState, EmailSetupSideEffect> {

    override val container = container<EmailSetupState, EmailSetupSideEffect>(EmailSetupState()) {
        // Pre-fill fields if credentials are already saved
        intent {
            val saved = credentialRepository.get()
            if (saved != null) {
                reduce {
                    state.copy(
                        address      = saved.address,
                        appPassword  = saved.appPassword,
                        isConfigured = true,
                    )
                }
            }
        }
    }

    fun onIntent(intent: EmailSetupIntent) = when (intent) {

        is EmailSetupIntent.UpdateAddress -> intent {
            reduce { state.copy(address = intent.value, testResult = null) }
        }

        is EmailSetupIntent.UpdateAppPassword -> intent {
            reduce { state.copy(appPassword = intent.value, testResult = null) }
        }

        EmailSetupIntent.TogglePasswordVisibility -> intent {
            reduce { state.copy(isPasswordVisible = !state.isPasswordVisible) }
        }

        EmailSetupIntent.DismissTestResult -> intent {
            reduce { state.copy(testResult = null) }
        }

        // ── Test + Save ───────────────────────────────────────────────────────

        EmailSetupIntent.TestAndSave -> intent {
            val address     = state.address.trim()
            val appPassword = state.appPassword.replace(" ", "")

            if (address.isBlank() || appPassword.isBlank()) {
                postSideEffect(EmailSetupSideEffect.ShowSnackbar("Please enter both Gmail address and App Password"))
                return@intent
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()) {
                postSideEffect(EmailSetupSideEffect.ShowSnackbar("Please enter a valid Gmail address"))
                return@intent
            }

            reduce { state.copy(isLoading = true, testResult = null) }

            when (val result = emailSender.testConnection(address, appPassword)) {
                is EmailSendResult.Success -> {
                    credentialRepository.save(address, appPassword)
                    reduce {
                        state.copy(
                            isLoading    = false,
                            isConfigured = true,
                            testResult   = TestResult.Success,
                        )
                    }
                }
                is EmailSendResult.Failure -> {
                    reduce {
                        state.copy(
                            isLoading  = false,
                            testResult = TestResult.Failure(result.reason),
                        )
                    }
                }
            }
        }

        // ── Clear ─────────────────────────────────────────────────────────────

        EmailSetupIntent.ClearCredentials -> intent {
            credentialRepository.clear()
            reduce {
                state.copy(
                    address      = "",
                    appPassword  = "",
                    isConfigured = false,
                    testResult   = null,
                )
            }
            postSideEffect(EmailSetupSideEffect.ShowSnackbar("Gmail account removed"))
        }

        // ── Navigation ────────────────────────────────────────────────────────

        EmailSetupIntent.NavigateBack -> intent {
            postSideEffect(EmailSetupSideEffect.GoBack)
        }
    }
}
