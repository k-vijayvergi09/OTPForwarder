package com.samsung.android.otpforwarder.core.sms

import com.samsung.android.otpforwarder.core.model.OtpEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-wide event bus for detected OTP events.
 *
 * [SmsReceiver] (a short-lived BroadcastReceiver) emits events here.
 * Collectors (WorkManager workers, ViewModels, ForegroundService) subscribe
 * to [events] to trigger forwarding, logging, and notifications.
 *
 * The backing [MutableSharedFlow] has:
 * - `replay = 0`  — new subscribers do not get past events (avoids duplicate forwarding).
 * - `extraBufferCapacity = 64` — absorbs burst of OTPs without blocking the receiver.
 */
@Singleton
class SmsEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<OtpEvent>(
        replay = 0,
        extraBufferCapacity = 64,
    )

    /** Hot [SharedFlow] of detected [OtpEvent]s. Subscribe on an appropriate dispatcher. */
    val events: SharedFlow<OtpEvent> = _events.asSharedFlow()

    /**
     * Emit [event] to all active subscribers.
     * Non-suspending — uses [MutableSharedFlow.tryEmit] so it is safe to call
     * from a [android.content.BroadcastReceiver.onReceive] without a coroutine scope.
     *
     * Returns `false` (and logs) if the internal buffer is full (capacity = 64).
     */
    fun emit(event: OtpEvent): Boolean = _events.tryEmit(event)
}
