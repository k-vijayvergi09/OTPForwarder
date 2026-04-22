package com.samsung.android.otpforwarder.core.sms

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

/**
 * Retains the latest observed incoming SMS so foreground UI can react even if
 * the receiver emits before the activity collector is actively listening.
 */
@Singleton
class IncomingSmsMonitor @Inject constructor() {

    private val _latestObservation = MutableStateFlow<IncomingSmsObservation?>(null)
    val latestObservation: StateFlow<IncomingSmsObservation?> = _latestObservation.asStateFlow()

    fun record(message: SmsMessage) {
        val nextSequence = (_latestObservation.value?.sequence ?: 0L) + 1L
        _latestObservation.value = IncomingSmsObservation(
            sequence = nextSequence,
            message = message,
        )
    }
}
