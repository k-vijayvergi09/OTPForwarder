package com.samsung.android.otpforwarder.core.sms

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.samsung.android.otpforwarder.core.domain.DetectOtpUseCase
import com.samsung.android.otpforwarder.core.domain.DevLogRepository
import com.samsung.android.otpforwarder.core.domain.ForwardingRepository
import com.samsung.android.otpforwarder.core.model.DevLogEntry
import com.samsung.android.otpforwarder.core.model.DevLogStage
import com.samsung.android.otpforwarder.core.model.DevLogStatus
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
 *
 * When Developer Mode is enabled, every pipeline step is also written to
 * [DevLogRepository] under the OTP event's ID so developers can export a
 * full trace from the Logs screen.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var smsParser: SmsParser
    @Inject lateinit var detectOtpUseCase: DetectOtpUseCase
    @Inject lateinit var forwardingRepository: ForwardingRepository
    @Inject lateinit var incomingSmsMonitor: IncomingSmsMonitor
    @Inject lateinit var incomingSmsNotifier: IncomingSmsNotifier
    @Inject lateinit var eventBus: SmsEventBus
    @Inject lateinit var devLogRepository: DevLogRepository

    override fun onReceive(context: Context, intent: Intent) {
        // This line fires even before Hilt injects fields — if you see it in logcat
        // the receiver IS being called. If you don't, the broadcast never reached us
        // (wrong permission state or OEM blocking).
        Timber.tag(TAG).i("onReceive action=${intent.action}")

        // Accumulate entries before we know the event ID.
        // Once we have an OtpEvent we flush these under the event's own key.
        val preEntries = mutableListOf<DevLogEntry>()
        fun preLog(
            stage: DevLogStage,
            status: DevLogStatus,
            message: String,
            detail: String? = null,
        ) {
            preEntries += DevLogEntry(
                timestamp = Clock.System.now(),
                stage     = stage,
                status    = status,
                message   = message,
                detail    = detail,
            )
        }

        preLog(
            stage   = DevLogStage.SMS_RECEIVED,
            status  = DevLogStatus.OK,
            message = "SMS broadcast received",
            detail  = "action=${intent.action}",
        )

        // ── Permission guard ──────────────────────────────────────────────────

        val permissionState = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECEIVE_SMS
        )
        if (permissionState != PackageManager.PERMISSION_GRANTED) {
            preLog(
                stage   = DevLogStage.PERMISSION_CHECK,
                status  = DevLogStatus.ERROR,
                message = "RECEIVE_SMS permission denied — broadcast ignored",
                detail  = "Grant it via App Settings → Permissions",
            )
            Timber.tag(TAG).i("RECEIVE_SMS not granted — ignoring broadcast (grant it via app Settings)")
            return
        }
        preLog(
            stage   = DevLogStage.PERMISSION_CHECK,
            status  = DevLogStatus.OK,
            message = "RECEIVE_SMS permission granted",
        )

        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Timber.tag(TAG).i("Ignoring unexpected action=${intent.action}")
            Timber.w("SmsReceiver: unexpected action %s - ignoring", intent.action)
            return
        }

        // ── Parsing ───────────────────────────────────────────────────────────

        val messages = smsParser.parse(intent)
        Timber.tag(TAG).i("Parsed message count=${messages.size}")

        if (messages.isEmpty()) {
            preLog(
                stage   = DevLogStage.SMS_PARSED,
                status  = DevLogStatus.WARN,
                message = "No parseable SMS found in broadcast",
                detail  = "PDU data may be missing or malformed",
            )
            Timber.tag(TAG).i("No parseable SMS found in broadcast")
            Timber.d("SmsReceiver: no parseable SMS in broadcast")
            return
        }
        preLog(
            stage   = DevLogStage.SMS_PARSED,
            status  = DevLogStatus.OK,
            message = "Parsed ${messages.size} message(s) from broadcast",
        )

        // ── Per-message processing ────────────────────────────────────────────

        for (message in messages) {
            Timber.tag(TAG).i("Received SMS sender=${message.sender} bodyLength=${message.body.length}")
            Timber.d(
                "SmsReceiver: received SMS from=%s length=%d",
                message.sender,
                message.body.length,
            )

            // Copy the pre-detection entries so each message gets its own trace.
            val msgEntries = preEntries.toMutableList()
            fun msgLog(
                stage: DevLogStage,
                status: DevLogStatus,
                message: String,
                detail: String? = null,
            ) {
                msgEntries += DevLogEntry(
                    timestamp = Clock.System.now(),
                    stage     = stage,
                    status    = status,
                    message   = message,
                    detail    = detail,
                )
            }

            // Always record raw message so the UI can react (snackbar, etc.)
            incomingSmsMonitor.record(message)
            Timber.tag(TAG).i("Recorded SMS in IncomingSmsMonitor sender=${message.sender}")
            msgLog(
                stage   = DevLogStage.MONITOR_RECORDED,
                status  = DevLogStatus.OK,
                message = "SMS recorded in IncomingSmsMonitor",
                detail  = "sender=${message.sender} bodyLength=${message.body.length}",
            )

            // ── OTP detection ─────────────────────────────────────────────────

            val otpEvent: OtpEvent? = detectOtpUseCase(message)
            if (otpEvent == null) {
                msgLog(
                    stage   = DevLogStage.OTP_DETECTION,
                    status  = DevLogStatus.WARN,
                    message = "No OTP detected in message from ${message.sender} — skipping forwarding",
                    detail  = "Message did not match OTP keyword/pattern rules",
                )
                Timber.tag(TAG).i("No OTP detected in message from ${message.sender}, skipping forwarding")
                continue
            }

            // We now have a stable event ID — flush all accumulated entries.
            msgLog(
                stage   = DevLogStage.OTP_DETECTION,
                status  = DevLogStatus.OK,
                message = "OTP detected: ${otpEvent.otpCode}",
                detail  = "sender=${otpEvent.sender} eventId=${otpEvent.id}",
            )
            msgEntries.forEach { devLogRepository.log(otpEvent.id, it) }

            // ── Notification ──────────────────────────────────────────────────

            // Only notify for confirmed OTP messages — avoids leaking the content of
            // personal texts, delivery receipts, and promotional messages.
            incomingSmsNotifier.notifyIncomingMessage(
                sender  = message.sender,
                preview = message.body.take(120),
            )
            Timber.tag(TAG).i("Requested notification for sender=${message.sender}")
            devLogRepository.log(otpEvent.id, DevLogEntry(
                timestamp = Clock.System.now(),
                stage     = DevLogStage.NOTIFICATION_TRIGGERED,
                status    = DevLogStatus.OK,
                message   = "Incoming OTP notification triggered",
                detail    = "sender=${message.sender}",
            ))

            // ── Persist ───────────────────────────────────────────────────────

            Timber.tag(TAG).i("Forwarding SMS sender=${otpEvent.sender}")
            Timber.i(
                "SmsReceiver: forwarding code=%s sender=%s",
                otpEvent.otpCode,
                otpEvent.sender,
            )

            forwardingRepository.recordDetectedOtp(otpEvent)
            Timber.tag(TAG).i("Persisted event id=${otpEvent.id}")
            devLogRepository.log(otpEvent.id, DevLogEntry(
                timestamp = Clock.System.now(),
                stage     = DevLogStage.REPOSITORY_RECORDED,
                status    = DevLogStatus.OK,
                message   = "OTP event persisted to repository (status=PENDING)",
                detail    = "id=${otpEvent.id}",
            ))

            // ── Event bus ─────────────────────────────────────────────────────

            val emitted = eventBus.emit(otpEvent)
            Timber.tag(TAG).i("EventBus emit emitted=$emitted for id=${otpEvent.id}")
            devLogRepository.log(otpEvent.id, DevLogEntry(
                timestamp = Clock.System.now(),
                stage     = DevLogStage.EVENT_BUS_EMITTED,
                status    = if (emitted) DevLogStatus.OK else DevLogStatus.WARN,
                message   = if (emitted)
                    "OTP event emitted to SmsEventBus — ForwardingDispatcher will enqueue worker"
                else
                    "SmsEventBus buffer full — event dropped (ForwardingDispatcher will retry on next startup)",
                detail    = "emitted=$emitted eventId=${otpEvent.id}",
            ))

            if (!emitted) {
                Timber.w("SmsReceiver: eventBus buffer full — event dropped id=%s", otpEvent.id)
            }
        }
    }
}
