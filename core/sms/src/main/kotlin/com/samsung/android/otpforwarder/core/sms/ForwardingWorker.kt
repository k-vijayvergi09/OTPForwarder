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
import com.samsung.android.otpforwarder.core.domain.ForwardingRepository
import com.samsung.android.otpforwarder.core.domain.SettingsRepository
import com.samsung.android.otpforwarder.core.domain.SmsDestinationRepository
import com.samsung.android.otpforwarder.core.model.DestinationType
import com.samsung.android.otpforwarder.core.model.ForwardingStatus
import com.samsung.android.otpforwarder.core.model.SmsDestination
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Background worker that forwards a single detected OTP to every enabled
 * [SmsDestination].
 *
 * As of the Rules → Destinations migration (2026-05-13), there is no per-rule
 * routing: every detected OTP fans out to every destination with
 * [SmsDestination.isEnabled] = true. Email destinations are persisted by the
 * Destinations feature but not yet consumed here (see milestone M3).
 *
 * Outcome rules:
 *  - 0 enabled SMS destinations → fail-success (status FAILED with explanation, Result.success())
 *  - ≥1 destinations, at least one delivers → record FORWARDED, channels = [SMS]
 *  - ≥1 destinations, none deliver → retry up to [MAX_ATTEMPTS]; then FAILED
 *
 * Per-destination success is best-effort: a single dispatch failure to any one
 * number is treated as a worker-level failure to keep the WorkManager retry
 * semantics simple. We can split into per-destination work items later if
 * partial-success retries become a requirement.
 */
@HiltWorker
class ForwardingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val forwardingRepository: ForwardingRepository,
    private val settingsRepository: SettingsRepository,
    private val smsDestinationRepository: SmsDestinationRepository,
    private val smsSender: SmsSender,
    private val forwardingNotifier: ForwardingNotifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val eventId = inputData.getString(KEY_EVENT_ID)
        if (eventId == null) {
            Timber.e("ForwardingWorker: No event ID provided")
            return Result.failure()
        }

        val settings = settingsRepository.settings.first()
        if (!settings.isForwardingEnabled) {
            Timber.i("ForwardingWorker: Forwarding is disabled globally")
            forwardingRepository.updateStatus(eventId, ForwardingStatus.FAILED, "Forwarding disabled")
            return Result.success()
        }

        val destinations = smsDestinationRepository.enabledOnce()
            .filter { PhoneNumberValidator.isValid(it.phoneNumber) }

        if (destinations.isEmpty()) {
            Timber.w("ForwardingWorker: No enabled SMS destinations configured for event $eventId")
            forwardingRepository.updateStatus(
                eventId,
                ForwardingStatus.FAILED,
                "No enabled SMS destination configured",
            )
            // Treat as success at the WorkManager level — retrying won't help if
            // the user simply hasn't added a destination yet.
            return Result.success()
        }

        // Prefer input data — ForwardingDispatcher now passes sender + fullBody directly
        // so the worker is not subject to a race with the Room upsert in SmsReceiver.
        // Fall back to the DB lookup for work requests that pre-date this change.
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

        // Fan out to every enabled destination. Treat the worker as successful if
        // *at least one* dispatch succeeds — partial delivery still beats holding
        // an OTP back from the user.
        var anySucceeded = false
        val failureLabels = mutableListOf<String>()

        destinations.forEach { destination ->
            val ok = smsSender.sendSms(destination.phoneNumber, messageBody)
            if (ok) {
                anySucceeded = true
                Timber.i(
                    "ForwardingWorker: delivered event %s to %s (%s)",
                    eventId, destination.phoneNumber, destination.label.ifBlank { "no label" },
                )
            } else {
                failureLabels += destination.label.ifBlank { destination.phoneNumber }
                Timber.w(
                    "ForwardingWorker: dispatch failed for event %s → %s",
                    eventId, destination.phoneNumber,
                )
            }
        }

        // Stable notification ID for this event — same across retries so Android
        // replaces the failure notification in-place rather than stacking new ones.
        val failureNotificationId = eventId.hashCode()

        return if (anySucceeded) {
            forwardingNotifier.cancelFailureNotification(failureNotificationId)
            forwardingRepository.updateDestinations(eventId, listOf(DestinationType.SMS))
            forwardingRepository.updateStatus(eventId, ForwardingStatus.FORWARDED, null)

            // Use the first delivered destination as the notification subtitle, or
            // a count if several were targeted.
            val notifyTarget = when (destinations.size) {
                1    -> destinations.first().phoneNumber
                else -> "${destinations.size} destinations"
            }
            forwardingNotifier.notifyForwarded(
                sender      = sender,
                destination = notifyTarget,
            )
            Result.success()
        } else {
            val attemptNumber = runAttemptCount + 1
            val isFinalAttempt = runAttemptCount >= MAX_ATTEMPTS - 1
            val errorDetail = if (failureLabels.size == 1) {
                "SMS dispatch failed to ${failureLabels.first()}"
            } else {
                "SMS dispatch failed to ${failureLabels.size} destinations"
            }

            if (isFinalAttempt) {
                Timber.w("ForwardingWorker: all $MAX_ATTEMPTS attempts exhausted for event $eventId")
                forwardingRepository.updateStatus(
                    eventId,
                    ForwardingStatus.FAILED,
                    "$errorDetail after $MAX_ATTEMPTS attempts",
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

    /**
     * Required by WorkManager when [setExpedited] is used.
     *
     * On API 31+ (our minSdk = 33) expedited work runs as a high-priority job —
     * not a foreground service — so this method is only a safety fallback that
     * will never actually be called in production. It is still required by the
     * WorkManager API contract.
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

        private const val CHANNEL_ID         = "forwarding_events"
        private const val FOREGROUND_NOTIF_ID = 8888
    }
}
