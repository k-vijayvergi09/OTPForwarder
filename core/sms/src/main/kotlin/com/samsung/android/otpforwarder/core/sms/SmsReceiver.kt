package com.samsung.android.otpforwarder.core.sms

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import com.samsung.android.otpforwarder.core.domain.DetectOtpUseCase
import com.samsung.android.otpforwarder.core.domain.ForwardingRepository
import com.samsung.android.otpforwarder.core.model.OtpEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.datetime.Clock
import timber.log.Timber
import javax.inject.Inject

private const val TAG = "SmsReceiver"

/**
 * Listens for incoming SMS messages and pipes detected OTPs into [SmsEventBus].
 *
 * WorkManager enqueueing is intentionally NOT done here — [ForwardingDispatcher]
 * collects from [SmsEventBus] in ApplicationScope and handles that responsibility.
 * Keeping the receiver focused on parse→detect→record keeps it fast and testable.
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
        // This line fires even before Hilt injects fields — if you see it in logcat
        // the receiver IS being called. If you don't, the broadcast never reached us
        // (wrong permission state or OEM blocking).
        Timber.tag(TAG).i("onReceive action=${intent.action}")

        // Guard: RECEIVE_SMS must be granted at runtime (Android 6+).
        val permissionState = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECEIVE_SMS
        )
        if (permissionState != PackageManager.PERMISSION_GRANTED) {
            Timber.tag(TAG).i("RECEIVE_SMS not granted — ignoring broadcast (grant it via app Settings)")
            return
        }
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Timber.tag(TAG).i("Ignoring unexpected action=${intent.action}")
            Timber.w("SmsReceiver: unexpected action %s - ignoring", intent.action)
            return
        }

        val messages = smsParser.parse(intent)
        Timber.tag(TAG).i("Parsed message count=${messages.size}")
        if (messages.isEmpty()) {
            Timber.tag(TAG).i("No parseable SMS found in broadcast")
            Timber.d("SmsReceiver: no parseable SMS in broadcast")
            return
        }

        for (message in messages) {
            Timber.tag(TAG).i("Received SMS sender=${message.sender} bodyLength=${message.body.length}")
            Timber.d(
                "SmsReceiver: received SMS from=%s length=%d",
                message.sender,
                message.body.length,
            )

            // Always record the raw message so the UI can react (snackbar, etc.)
            incomingSmsMonitor.record(message)
            Timber.tag(TAG).i("Recorded SMS in IncomingSmsMonitor sender=${message.sender}")

            val otpEvent = detectOtpUseCase(message)
            if (otpEvent == null) {
                Timber.tag(TAG).i("No OTP detected in message from ${message.sender}, skipping forwarding")
                continue
            }

            // Only notify for confirmed OTP messages — avoids leaking the content of
            // personal texts, delivery receipts, and promotional messages to the
            // notification shade.
            incomingSmsNotifier.notifyIncomingMessage(
                sender  = message.sender,
                preview = message.body.take(120),
            )
            Timber.tag(TAG).i("Requested notification for sender=${message.sender}")

            Timber.tag(TAG).i("Forwarding SMS sender=${otpEvent.sender}")
            Timber.i(
                "SmsReceiver: forwarding code=%s sender=%s",
                otpEvent.otpCode,
                otpEvent.sender,
            )

            // Persist the detection so the Logs/Home screens update immediately.
            forwardingRepository.recordDetectedOtp(otpEvent)
            Timber.tag(TAG).i("Persisted event id=${otpEvent.id}")

            // Broadcast to ForwardingDispatcher (ApplicationScope collector) which
            // will enqueue ForwardingWorker. Using tryEmit here is safe because
            // SmsEventBus has a 64-event buffer and ForwardingDispatcher is always
            // running while the process is alive.
            val emitted = eventBus.emit(otpEvent)
            Timber.tag(TAG).i("EventBus emit emitted=$emitted for id=${otpEvent.id}")
            if (!emitted) {
                Timber.w("SmsReceiver: eventBus buffer full — event dropped id=%s", otpEvent.id)
            }
        }
    }
}
