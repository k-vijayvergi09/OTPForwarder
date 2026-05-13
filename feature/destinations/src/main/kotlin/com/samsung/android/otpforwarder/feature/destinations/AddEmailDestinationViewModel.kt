package com.samsung.android.otpforwarder.feature.destinations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.otpforwarder.core.domain.EmailDestinationRepository
import com.samsung.android.otpforwarder.core.model.EmailDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddEmailDestinationViewModel @Inject constructor(
    private val emailRepo: EmailDestinationRepository,
) : ViewModel(), ContainerHost<AddEmailState, AddEmailSideEffect> {

    override val container = container<AddEmailState, AddEmailSideEffect>(AddEmailState())

    fun onIntent(intent: AddEmailIntent) = when (intent) {
        is AddEmailIntent.LabelChanged   -> intent { reduce { state.copy(label = intent.value) } }
        is AddEmailIntent.AddressChanged -> intent { reduce { state.copy(emailAddress = intent.value, emailError = null) } }
        AddEmailIntent.ToggleEnable      -> intent { reduce { state.copy(enableImmediately = !state.enableImmediately) } }
        AddEmailIntent.NavigateBack      -> intent { postSideEffect(AddEmailSideEffect.GoBack) }
        AddEmailIntent.Save              -> save()
    }

    private fun save() = intent {
        val trimmedAddress = state.emailAddress.trim()
        if (trimmedAddress.isBlank() || !trimmedAddress.contains('@')) {
            reduce { state.copy(emailError = "Enter a valid email address") }
            return@intent
        }

        reduce { state.copy(isSaving = true, emailError = null) }

        viewModelScope.launch {
            emailRepo.upsert(
                EmailDestination(
                    id           = UUID.randomUUID().toString(),
                    label        = state.label.trim(),
                    emailAddress = trimmedAddress,
                    isEnabled    = state.enableImmediately,
                )
            )
        }.join()

        postSideEffect(AddEmailSideEffect.GoBack)
    }
}
