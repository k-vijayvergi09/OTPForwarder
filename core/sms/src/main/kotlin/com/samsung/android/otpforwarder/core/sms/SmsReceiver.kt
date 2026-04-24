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
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

private const val TAG = "SmsReceiver"

/**
 * Listens for incoming SMS messages and pipes detected OTPs into [SmsEventBus].
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
        Log.i(TAG, "onReceive action=${intent.action}")

        // Guard: RECEIVE_SMS must be granted at runtime (Android 6+).
        val permissionState = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECEIVE_SMS
        )
        if (permissionState != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECEIVE_SMS not granted — ignoring broadcast (grant it via app Settings)")
            return
        }
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.i(TAG, "Ignoring unexpected action=${intent.action}")
            Timber.w("SmsReceiver: unexpected action %s - ignoring", intent.action)
            return
        }

        val messages = smsParser.parse(intent)
        Log.i(TAG, "Parsed message count=${messages.size}")
        if (messages.isEmpty()) {
            Log.i(TAG, "No parseable SMS found in broadcast")
            Timber.d("SmsReceiver: no parseable SMS in broadcast")
            return
        }

        for (message in messages) {
            Log.i(TAG, "Received SMS sender=${message.sender} bodyLength=${message.body.length}")
            Timber.d(
                "SmsReceiver: received SMS from=%s length=%d",
                message.sender,
                message.body.length,
            )

            // Always record the raw message so the UI can react (snackbar, etc.)
            incomingSmsMonitor.record(message)
            Log.i(TAG, "Recorded SMS in IncomingSmsMonitor sender=${message.sender}")

            // Post a system notification for every incoming SMS (not just OTPs)
            incomingSmsNotifier.notifyIncomingMessage(
                sender  = message.sender,
                preview = message.body.take(120),
            )
            Log.i(TAG, "Requested notification for sender=${message.sender}")

            // OTP detection — only forward if a code is found
            val otpEvent = detectOtpUseCase(message)
            if (otpEvent != null) {
                Log.i(TAG, "OTP detected code=${otpEvent.otpCode} sender=${otpEvent.sender}")
                Timber.i(
                    "SmsReceiver: OTP detected code=%s sender=%s",
                    otpEvent.otpCode,
                    otpEvent.sender,
                )

                // Persist the detection so the Logs/Home screens update immediately
                forwardingRepository.recordDetectedOtp(otpEvent)
                Log.i(TAG, "Persisted OTP event id=${otpEvent.id}")

                // Broadcast to subscribers (WorkManager workers, forwarding service, etc.)
                val emitted = eventBus.emit(otpEvent)
                Log.i(TAG, "EventBus emit emitted=$emitted for id=${otpEvent.id}")
                if (!emitted) {
                    Timber.w("SmsReceiver: eventBus buffer full — OTP event dropped id=%s", otpEvent.id)
                }
            } else {
                Log.i(TAG, "No OTP detected in SMS from sender=${message.sender}")
                Timber.d("SmsReceiver: no OTP in message from sender=%s", message.sender)
            }
        }
    }
}
