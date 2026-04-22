package com.samsung.android.otpforwarder.core.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.samsung.android.otpforwarder.core.domain.DetectOtpUseCase
import com.samsung.android.otpforwarder.core.domain.ForwardingRepository
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Listens for incoming SMS messages and pipes detected OTPs into [SmsEventBus].
 *
 * Registered in [AndroidManifest.xml] with:
 *   - `android.provider.Telephony.SMS_RECEIVED` intent action
 *   - `android:priority="999"` — highest possible to receive before other apps
 *   - `android:exported="true"` — required for system broadcasts
 *
 * **Lifecycle note:** [BroadcastReceiver.onReceive] runs on the main thread and
 * must complete quickly. Parsing and OTP detection are O(n) regex operations on
 * short strings — safe to do inline. Any I/O (Room insert, WorkManager enqueue)
 * must be deferred to a coroutine or Worker kicked off after this call returns.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var smsParser: SmsParser
    @Inject lateinit var detectOtpUseCase: DetectOtpUseCase
    @Inject lateinit var forwardingRepository: ForwardingRepository
    @Inject lateinit var incomingSmsMonitor: IncomingSmsMonitor
    @Inject lateinit var incomingSmsNotifier: IncomingSmsNotifier
    @Inject lateinit var eventBus: SmsEventBus

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Timber.w("SmsReceiver: unexpected action %s — ignoring", intent.action)
            return
        }

        val messages = smsParser.parse(intent)
        if (messages.isEmpty()) {
            Timber.d("SmsReceiver: no parseable SMS in broadcast")
            return
        }

        for (message in messages) {
            Timber.d(
                "SmsReceiver: received SMS from=%s length=%d",
                message.sender,
                message.body.length,
            )
            incomingSmsMonitor.record(message)
            incomingSmsNotifier.notifyIncomingMessage(
                sender = message.sender,
                preview = message.body.take(120),
            )
            val otpEvent = detectOtpUseCase(message)
            if (otpEvent != null) {
                Timber.i(
                    "SmsReceiver: OTP detected code=%s from=%s",
                    otpEvent.otpCode,
                    otpEvent.sender,
                )
                forwardingRepository.recordDetectedOtp(otpEvent)
                val emitted = eventBus.emit(otpEvent)
                if (!emitted) {
                    Timber.e("SmsReceiver: event bus buffer full — OTP event dropped!")
                }
            } else {
                Timber.v("SmsReceiver: no OTP in message from %s", message.sender)
            }
        }
    }
}
