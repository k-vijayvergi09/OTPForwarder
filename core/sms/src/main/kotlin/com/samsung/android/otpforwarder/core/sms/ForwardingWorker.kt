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
import com.samsung.android.otpforwarder.core.model.DestinationType
import com.samsung.android.otpforwarder.core.model.ForwardingStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

@HiltWorker
class ForwardingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val forwardingRepository: ForwardingRepository,
    private val settingsRepository: SettingsRepository,
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

        if (DestinationType.SMS !in settings.defaultDestinations) {
            Timber.i("ForwardingWorker: SMS destination not enabled")
            // Update status so the UI doesn't show PENDING indefinitely.
            forwardingRepository.updateStatus(eventId, ForwardingStatus.FAILED, "SMS destination not enabled")
            return Result.success()
        }

        val destinationNumber = settings.defaultPhoneNumber
        if (destinationNumber.isBlank()) {
            Timber.w("ForwardingWorker: SMS destination enabled but no number configured")
            forwardingRepository.updateStatus(eventId, ForwardingStatus.FAILED, "No destination number")
            return Result.failure()
        }
        if (!PhoneNumberValidator.isValid(destinationNumber)) {
            Timber.w("ForwardingWorker: Destination number failed validation: $destinationNumber")
            forwardingRepository.updateStatus(eventId, ForwardingStatus.FAILED, "Invalid destination number")
            return Result.failure()
        }

        // Prefer input data — ForwardingDispatcher now passes sender + fullBody directly
        // so the worker is not subject to a race with the Room upsert in SmsReceiver.
        // Fall back to the DB lookup for work requests that pre-date this change.
        val sender = inputData.getString(KEY_SENDER)
            ?: forwardingRepository.records.first().find { it.id == eventId }?.sender
            ?: run {
                // Record not in the StateFlow yet — the DB upsert is still in-flight.
                // Retry after the backoff period; it will be there by then.
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
        val success = smsSender.sendSms(destinationNumber, messageBody)

        // Stable notification ID for this event — same across retries so Android
        // replaces the failure notification in-place rather than stacking new ones.
        val failureNotificationId = eventId.hashCode()

        return if (success) {
            // Dismiss any lingering failure/retry notification from earlier attempts.
            forwardingNotifier.cancelFailureNotification(failureNotificationId)

            forwardingRepository.updateDestinations(eventId, listOf(DestinationType.SMS))
            forwardingRepository.updateStatus(eventId, ForwardingStatus.FORWARDED, null)
            forwardingNotifier.notifyForwarded(
                sender      = sender,
                destination = destinationNumber,
            )
            Result.success()
        } else {
            // runAttemptCount is 0-indexed: 0 = first try, 1 = first retry, …
            val attemptNumber = runAttemptCount + 1
            val isFinalAttempt = runAttemptCount >= MAX_ATTEMPTS - 1

            if (isFinalAttempt) {
                Timber.w("ForwardingWorker: all $MAX_ATTEMPTS attempts exhausted for event $eventId")
                forwardingRepository.updateStatus(
                    eventId,
                    ForwardingStatus.FAILED,
                    "SMS dispatch failed after $MAX_ATTEMPTS attempts",
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
