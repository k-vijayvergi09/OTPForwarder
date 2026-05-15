package com.samsung.android.otpforwarder.core.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.samsung.android.otpforwarder.core.common.validation.PhoneNumberValidator
import com.samsung.android.otpforwarder.core.domain.DevLogRepository
import com.samsung.android.otpforwarder.core.domain.EmailDestinationRepository
import com.samsung.android.otpforwarder.core.domain.ForwardingRepository
import com.samsung.android.otpforwarder.core.domain.SettingsRepository
import com.samsung.android.otpforwarder.core.domain.SmsDestinationRepository
import com.samsung.android.otpforwarder.core.model.DestinationType
import com.samsung.android.otpforwarder.core.model.DevLogEntry
import com.samsung.android.otpforwarder.core.model.DevLogStage
import com.samsung.android.otpforwarder.core.model.DevLogStatus
import com.samsung.android.otpforwarder.core.model.ForwardingStatus
import com.samsung.android.otpforwarder.core.model.SmsDestination
import com.samsung.android.otpforwarder.core.network.EmailSendResult
import com.samsung.android.otpforwarder.core.network.EmailSender
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import timber.log.Timber

/**
 * Background worker that forwards a single detected OTP to every enabled
 * [SmsDestination] and every enabled email destination.
 *
 * Outcome rules:
 *  - 0 enabled destinations (SMS + email combined) → FAILED, Result.success()
 *  - ≥1 destinations, at least one delivers       → FORWARDED, channels recorded
 *  - ≥1 destinations, none deliver                → retry up to [MAX_ATTEMPTS]; then FAILED
 *
 * SMS and email fan-out are both best-effort: the worker succeeds if *any*
 * channel delivers. Failed channels are logged but do not block the others.
 *
 * Every significant step is also recorded in [DevLogRepository] so a developer
 * with Dev Mode enabled can export a full trace from the Logs screen.
 */
@HiltWorker
class ForwardingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val forwardingRepository: ForwardingRepository,
    private val settingsRepository: SettingsRepository,
    private val smsDestinationRepository: SmsDestinationRepository,
    private val emailDestinationRepository: EmailDestinationRepository,
    private val smsSender: SmsSender,
    private val emailSender: EmailSender,
    private val forwardingNotifier: ForwardingNotifier,
    private val devLogRepository: DevLogRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val eventId = inputData.getString(KEY_EVENT_ID)
        if (eventId == null) {
            Timber.e("ForwardingWorker: No event ID provided")
            return Result.failure()
        }

        val attemptNumber = runAttemptCount + 1

        fun devLog(
            stage: DevLogStage,
            status: DevLogStatus,
            message: String,
            detail: String? = null,
        ) {
            devLogRepository.log(eventId, DevLogEntry(
                timestamp = Clock.System.now(),
                stage     = stage,
                status    = status,
                message   = message,
                detail    = detail,
            ))
        }

        devLog(
            stage   = DevLogStage.FORWARDING_STARTED,
            status  = DevLogStatus.OK,
            message = "ForwardingWorker started (attempt $attemptNumber/$MAX_ATTEMPTS)",
            detail  = "eventId=$eventId",
        )

        // ── Settings check ────────────────────────────────────────────────────

        val settings = settingsRepository.settings.first()
        if (!settings.isForwardingEnabled) {
            devLog(
                stage   = DevLogStage.SETTINGS_CHECKED,
                status  = DevLogStatus.WARN,
                message = "Forwarding is disabled globally — marking event as FAILED",
            )
            Timber.i("ForwardingWorker: Forwarding is disabled globally")
            forwardingRepository.updateStatus(eventId, ForwardingStatus.FAILED, "Forwarding disabled")
            return Result.success()
        }
        devLog(
            stage   = DevLogStage.SETTINGS_CHECKED,
            status  = DevLogStatus.OK,
            message = "Forwarding is enabled globally",
            detail  = "delaySeconds=${settings.forwardingDelaySeconds}",
        )

        // ── Destinations ──────────────────────────────────────────────────────

        val smsDestinations = smsDestinationRepository.enabledOnce()
            .filter { PhoneNumberValidator.isValid(it.phoneNumber) }
        val emailDestinations = emailDestinationRepository.enabledOnce()

        if (smsDestinations.isEmpty() && emailDestinations.isEmpty()) {
            devLog(
                stage   = DevLogStage.DESTINATIONS_LOADED,
                status  = DevLogStatus.ERROR,
                message = "No enabled destinations found — marking event as FAILED",
                detail  = "Configure at least one SMS or email destination in the Destinations screen",
            )
            Timber.w("ForwardingWorker: No enabled destinations configured for event $eventId")
            forwardingRepository.updateStatus(
                eventId,
                ForwardingStatus.FAILED,
                "No enabled destinations configured",
            )
            return Result.success()
        }
        devLog(
            stage   = DevLogStage.DESTINATIONS_LOADED,
            status  = DevLogStatus.OK,
            message = "Loaded ${smsDestinations.size} SMS + ${emailDestinations.size} email destination(s)",
            detail  = buildString {
                if (smsDestinations.isNotEmpty()) {
                    append("SMS: ")
                    append(smsDestinations.joinToString { it.label.ifBlank { it.phoneNumber } })
                }
                if (emailDestinations.isNotEmpty()) {
                    if (isNotEmpty()) append(" | ")
                    append("Email: ")
                    append(emailDestinations.joinToString { it.label.ifBlank { it.emailAddress } })
                }
            },
        )

        // ── Resolve sender / body ─────────────────────────────────────────────

        val sender = inputData.getString(KEY_SENDER)
            ?: forwardingRepository.records.first().find { it.id == eventId }?.sender
            ?: run {
                Timber.w("ForwardingWorker: Record not found for id $eventId — retrying")
                return Result.retry()
            }

        val fullBody = inputData.getString(KEY_FULL_BODY)
            ?: forwardingRepository.records.first().find { it.id == eventId }?.fullBody
            ?: run {
                Timber.w("ForwardingWorker: Record not found for fullBody (id=$eventId) — retrying")
                return Result.retry()
            }

        val messageBody = "FWD from $sender: $fullBody"

        // ── SMS fan-out ───────────────────────────────────────────────────────

        var smsSucceeded = false
        val smsFailureLabels = mutableListOf<String>()

        smsDestinations.forEach { destination ->
            val ok = smsSender.sendSms(destination.phoneNumber, messageBody)
            val destLabel = destination.label.ifBlank { destination.phoneNumber }
            if (ok) {
                smsSucceeded = true
                devLog(
                    stage   = DevLogStage.SMS_DISPATCH,
                    status  = DevLogStatus.OK,
                    message = "SMS delivered to $destLabel",
                    detail  = "destination=${destination.phoneNumber}",
                )
                Timber.i(
                    "ForwardingWorker: SMS delivered event %s → %s (%s)",
                    eventId, destination.phoneNumber, destination.label.ifBlank { "no label" },
                )
            } else {
                smsFailureLabels += destLabel
                devLog(
                    stage   = DevLogStage.SMS_DISPATCH,
                    status  = DevLogStatus.ERROR,
                    message = "SMS dispatch failed to $destLabel",
                    detail  = "destination=${destination.phoneNumber}",
                )
                Timber.w(
                    "ForwardingWorker: SMS dispatch failed event %s → %s",
                    eventId, destination.phoneNumber,
                )
            }
        }

        // ── Email fan-out ─────────────────────────────────────────────────────

        var emailSucceeded = false
        val emailFailureLabels = mutableListOf<String>()

        emailDestinations.forEach { destination ->
            val result = emailSender.send(
                to      = destination.emailAddress,
                subject = "OTP from $sender",
                body    = messageBody,
            )
            val destLabel = destination.label.ifBlank { destination.emailAddress }
            when (result) {
                is EmailSendResult.Success -> {
                    emailSucceeded = true
                    devLog(
                        stage   = DevLogStage.EMAIL_DISPATCH,
                        status  = DevLogStatus.OK,
                        message = "Email delivered to $destLabel",
                        detail  = "destination=${destination.emailAddress}",
                    )
                    Timber.i(
                        "ForwardingWorker: email delivered event %s → %s (%s)",
                        eventId, destination.emailAddress, destination.label.ifBlank { "no label" },
                    )
                }
                is EmailSendResult.Failure -> {
                    emailFailureLabels += destLabel
                    devLog(
                        stage   = DevLogStage.EMAIL_DISPATCH,
                        status  = DevLogStatus.ERROR,
                        message = "Email dispatch failed to $destLabel: ${result.reason}",
                        detail  = "destination=${destination.emailAddress}",
                    )
                    Timber.w(
                        "ForwardingWorker: email dispatch failed event %s → %s: %s",
                        eventId, destination.emailAddress, result.reason,
                    )
                }
            }
        }

        // ── Outcome ───────────────────────────────────────────────────────────

        val anySucceeded = smsSucceeded || emailSucceeded
        val failureNotificationId = eventId.hashCode()

        return if (anySucceeded) {
            forwardingNotifier.cancelFailureNotification(failureNotificationId)

            val deliveredChannels = buildList {
                if (smsSucceeded)   add(DestinationType.SMS)
                if (emailSucceeded) add(DestinationType.EMAIL)
            }
            forwardingRepository.updateDestinations(eventId, deliveredChannels)
            forwardingRepository.updateStatus(eventId, ForwardingStatus.FORWARDED, null)

            devLog(
                stage   = DevLogStage.FORWARDING_COMPLETE,
                status  = DevLogStatus.OK,
                message = "All forwarding complete — status updated to FORWARDED",
                detail  = "deliveredChannels=${deliveredChannels.joinToString { it.name }}",
            )

            val notifyTarget = buildNotifyTarget(
                smsDestinations.size,
                emailDestinations.size,
                smsSucceeded,
                emailSucceeded,
            )
            forwardingNotifier.notifyForwarded(sender = sender, destination = notifyTarget)
            Result.success()

        } else {
            val isFinalAttempt = runAttemptCount >= MAX_ATTEMPTS - 1
            val errorDetail    = buildErrorDetail(smsFailureLabels, emailFailureLabels)

            if (isFinalAttempt) {
                Timber.w("ForwardingWorker: all $MAX_ATTEMPTS attempts exhausted for event $eventId")
                forwardingRepository.updateStatus(
                    eventId,
                    ForwardingStatus.FAILED,
                    "$errorDetail after $MAX_ATTEMPTS attempts",
                )
                devLog(
                    stage   = DevLogStage.FORWARDING_COMPLETE,
                    status  = DevLogStatus.ERROR,
                    message = "All $MAX_ATTEMPTS attempts exhausted — status updated to FAILED",
                    detail  = errorDetail,
                )
                forwardingNotifier.notifySendFailed(
                    sender         = sender,
                    eventId        = eventId,
                    attemptNumber  = attemptNumber,
                    maxAttempts    = MAX_ATTEMPTS,
                    willRetry      = false,
                    notificationId = failureNotificationId,
                )
                Result.failure()
            } else {
                Timber.w("ForwardingWorker: attempt $attemptNumber/$MAX_ATTEMPTS failed for event $eventId — retrying")
                forwardingRepository.updateStatus(
                    eventId,
                    ForwardingStatus.RETRY_QUEUED,
                    "Attempt $attemptNumber failed, retrying",
                )
                devLog(
                    stage   = DevLogStage.FORWARDING_COMPLETE,
                    status  = DevLogStatus.WARN,
                    message = "Attempt $attemptNumber/$MAX_ATTEMPTS failed — status updated to RETRY_QUEUED",
                    detail  = "$errorDetail — WorkManager will retry with exponential backoff",
                )
                forwardingNotifier.notifySendFailed(
                    sender         = sender,
                    eventId        = eventId,
                    attemptNumber  = attemptNumber,
                    maxAttempts    = MAX_ATTEMPTS,
                    willRetry      = true,
                    notificationId = failureNotificationId,
                )
                Result.retry()
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildNotifyTarget(
        smsCount: Int,
        emailCount: Int,
        smsOk: Boolean,
        emailOk: Boolean,
    ): String {
        val parts = buildList {
            if (smsOk && smsCount > 0) add(if (smsCount == 1) "SMS" else "$smsCount SMS")
            if (emailOk && emailCount > 0) add(if (emailCount == 1) "Email" else "$emailCount emails")
        }
        return parts.joinToString(" + ").ifBlank { "destination" }
    }

    private fun buildErrorDetail(
        smsFailures: List<String>,
        emailFailures: List<String>,
    ): String {
        val parts = buildList {
            if (smsFailures.isNotEmpty())   add("SMS failed to ${smsFailures.joinToString()}")
            if (emailFailures.isNotEmpty()) add("Email failed to ${emailFailures.joinToString()}")
        }
        return parts.joinToString("; ").ifBlank { "all dispatches failed" }
    }

    /**
     * Required by WorkManager when [setExpedited] is used.
     *
     * On API 31+ (our minSdk = 33) expedited work runs as a high-priority job —
     * not a foreground service — so this method is only a safety fallback that
     * will never actually be called in production.
     */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        ensureForwardingChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Forwarding OTP…")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        return ForegroundInfo(FOREGROUND_NOTIF_ID, notification)
    }

    private fun ensureForwardingChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Forwarding Events",
                        NotificationManager.IMPORTANCE_LOW,
                    )
                )
            }
        }
    }

    companion object {
        const val KEY_EVENT_ID  = "event_id"
        const val KEY_SENDER    = "sender"
        const val KEY_FULL_BODY = "full_body"

        /**
         * Maximum number of send attempts (1 initial + [MAX_ATTEMPTS - 1] retries).
         * WorkManager applies exponential backoff between retries (30s base,
         * configured in [ForwardingDispatcher]).
         */
        const val MAX_ATTEMPTS = 3

        private const val CHANNEL_ID          = "forwarding_events"
        private const val FOREGROUND_NOTIF_ID = 8888
    }
}
