package com.samsung.android.otpforwarder.feature.destinations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.otpforwarder.core.domain.EmailDestinationRepository
import com.samsung.android.otpforwarder.core.domain.SmsDestinationRepository
import com.samsung.android.otpforwarder.core.model.EmailDestination
import com.samsung.android.otpforwarder.core.model.SmsDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class DestinationsViewModel @Inject constructor(
    private val smsRepo: SmsDestinationRepository,
    private val emailRepo: EmailDestinationRepository,
) : ViewModel(), ContainerHost<DestinationsState, DestinationsSideEffect> {

    override val container = container<DestinationsState, DestinationsSideEffect>(
        DestinationsState(isLoading = true)
    )

    init {
        observeDestinations()
    }

    // ── Observation ───────────────────────────────────────────────────────────

    private fun observeDestinations() {
        combine(
            smsRepo.observeAll(),
            emailRepo.observeAll(),
        ) { smsList, emailList ->
            Pair(smsList.map { it.toUiItem() }, emailList.map { it.toUiItem() })
        }.onEach { (sms, email) ->
            intent {
                reduce {
                    state.copy(
                        isLoading         = false,
                        smsDestinations   = sms,
                        emailDestinations = email,
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    // ── Intents ───────────────────────────────────────────────────────────────

    fun onIntent(intent: DestinationsIntent) = when (intent) {
        DestinationsIntent.AddPhone      -> intent { postSideEffect(DestinationsSideEffect.NavigateToAddPhone) }
        DestinationsIntent.AddEmail      -> intent { postSideEffect(DestinationsSideEffect.NavigateToAddEmail) }
        DestinationsIntent.NavigateToSettings -> intent { postSideEffect(DestinationsSideEffect.GoToSettings) }

        is DestinationsIntent.ToggleSms  -> viewModelScope.launch {
            val current = container.stateFlow.value.smsDestinations.find { it.id == intent.id }
            if (current != null) smsRepo.setEnabled(intent.id, !current.isEnabled)
        }
        is DestinationsIntent.ToggleEmail -> viewModelScope.launch {
            val current = container.stateFlow.value.emailDestinations.find { it.id == intent.id }
            if (current != null) emailRepo.setEnabled(intent.id, !current.isEnabled)
        }
        is DestinationsIntent.DeleteSms  -> viewModelScope.launch {
            smsRepo.delete(intent.id)
        }
        is DestinationsIntent.DeleteEmail -> viewModelScope.launch {
            emailRepo.delete(intent.id)
        }
    }
}

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun SmsDestination.toUiItem() = SmsDestinationUiItem(
    id          = id,
    label       = label,
    phoneNumber = phoneNumber,
    isEnabled   = isEnabled,
)

private fun EmailDestination.toUiItem() = EmailDestinationUiItem(
    id           = id,
    label        = label,
    emailAddress = emailAddress,
    isEnabled    = isEnabled,
)
