package com.samsung.android.otpforwarder.core.sms

import android.util.Log
import com.samsung.android.otpforwarder.core.model.SmsMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class IncomingSmsObservation(
    val sequence: Long,
    val message: SmsMessage,
)

@Singleton
class IncomingSmsMonitor @Inject constructor() {

    private val _latestObservation = MutableStateFlow<IncomingSmsObservation?>(null)
    val latestObservation: StateFlow<IncomingSmsObservation?> = _latestObservation.asStateFlow()

    fun record(message: SmsMessage) {
        val nextSequence = (_latestObservation.value?.sequence ?: 0L) + 1L
        Log.i(TAG, "Recording SMS sequence=$nextSequence sender=${message.sender}")
        _latestObservation.value = IncomingSmsObservation(
            sequence = nextSequence,
            message = message,
        )
    }

    private companion object {
        const val TAG = "OtpForwarderMonitor"
    }
}
