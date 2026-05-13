package com.samsung.android.otpforwarder.feature.destinations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.otpforwarder.core.domain.SmsDestinationRepository
import com.samsung.android.otpforwarder.core.model.SmsDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddPhoneDestinationViewModel @Inject constructor(
    private val smsRepo: SmsDestinationRepository,
) : ViewModel(), ContainerHost<AddPhoneState, AddPhoneSideEffect> {

    override val container = container<AddPhoneState, AddPhoneSideEffect>(AddPhoneState())

    fun onIntent(intent: AddPhoneIntent) = when (intent) {
        is AddPhoneIntent.LabelChanged  -> intent { reduce { state.copy(label = intent.value) } }
        is AddPhoneIntent.NumberChanged -> intent { reduce { state.copy(phoneNumber = intent.value, phoneError = null) } }
        AddPhoneIntent.ToggleEnable     -> intent { reduce { state.copy(enableImmediately = !state.enableImmediately) } }
        AddPhoneIntent.NavigateBack     -> intent { postSideEffect(AddPhoneSideEffect.GoBack) }
        AddPhoneIntent.Save             -> save()
    }

    private fun save() = intent {
        val trimmedNumber = state.phoneNumber.trim()
        if (trimmedNumber.isBlank()) {
            reduce { state.copy(phoneError = "Phone number is required") }
            return@intent
        }

        reduce { state.copy(isSaving = true, phoneError = null) }

        viewModelScope.launch {
            smsRepo.upsert(
                SmsDestination(
                    id          = UUID.randomUUID().toString(),
                    label       = state.label.trim(),
                    phoneNumber = trimmedNumber,
                    isEnabled   = state.enableImmediately,
                )
            )
        }.join()

        postSideEffect(AddPhoneSideEffect.GoBack)
    }
}
