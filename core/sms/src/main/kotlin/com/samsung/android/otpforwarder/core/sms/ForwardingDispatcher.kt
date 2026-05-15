package com.samsung.android.otpforwarder.core.sms

import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.samsung.android.otpforwarder.core.common.coroutines.ApplicationScope
import com.samsung.android.otpforwarder.core.domain.DevLogRepository
import com.samsung.android.otpforwarder.core.domain.ForwardingRepository
import com.samsung.android.otpforwarder.core.domain.SettingsRepository
import com.samsung.android.otpforwarder.core.model.DevLogEntry
import com.samsung.android.otpforwarder.core.model.DevLogStage
import com.samsung.android.otpforwarder.core.model.DevLogStatus
import com.samsung.android.otpforwarder.core.model.OtpEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges the [SmsEventBus] (short-lived broadcast path) with [WorkManager]
 * (durable background forwarding).
 *
 * Call [start] once from [Application.onCreate] after Hilt injection.
 * The coroutine lives in [ApplicationScope] — it outlives all ViewModels
 * and is cancelled only when the process dies.
 *
 * On startup, [start] first calls [retryStuckRecords] to re-enqueue any records
 * that are stuck in PENDING / RETRY_QUEUED — records where a prior WorkManager
 * run finished without updating the Room status (race-condition bug, now fixed).
 *
 * For each new [OtpEvent] emitted by [SmsEventBus]:
 *   1. Reads the current [forwardingDelaySeconds] from settings.
 *   2. Enqueues a [ForwardingWorker] as unique expedited work keyed by `event.id`
 *      ([ExistingWorkPolicy.KEEP]) to prevent duplicate delivery on re-broadcast.
 *   3. Applies exponential backoff so transient failures are retried automatically.
 */
@Singleton
class ForwardingDispatcher @Inject constructor(
    private val eventBus: SmsEventBus,
    private val workManager: WorkManager,
    private val settingsRepository: SettingsRepository,
    private val forwardingRepository: ForwardingRepository,
    private val devLogRepository: DevLogRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {

    /**
     * Start collecting OTP events and dispatching forwarding work.
     * Safe to call multiple times — subsequent calls are no-ops because
     * [SmsEventBus.events] is a hot [SharedFlow] and the first collector
     * is already running in [scope].
     *
     * Must be called after Hilt injection is complete (i.e. after
     * [Application.onCreate] super call).
     */
    fun start() {
        scope.launch {
            Timber.d("ForwardingDispatcher: started")

            // Re-enqueue any records that are stuck in PENDING / RETRY_QUEUED.
            // This handles records from before the bug-fix where WorkManager
            // finished without ever calling forwardingRepository.updateStatus().
            retryStuckRecords()

            // Then keep collecting new OTP events for the lifetime of the process.
            Timber.d("ForwardingDispatcher: collecting new OTP events")
            eventBus.events.collect { event ->
                enqueue(event)
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Queries the DB directly (one-shot suspend, not the StateFlow) so it is
     * safe to call before the [ForwardingRepository.records] StateFlow has
     * received its first Room emission.
     *
     * Uses [ExistingWorkPolicy.KEEP] — if WorkManager already has ENQUEUED or
     * RUNNING work for an ID (e.g. a legitimate RETRY_QUEUED record still backed
     * by pending WorkManager work), we leave it alone. If the prior work is in a
     * terminal state (FAILED / SUCCEEDED / CANCELLED), a fresh request is enqueued.
     */
    private suspend fun retryStuckRecords() {
        val stuck = forwardingRepository.pendingRecords()
        if (stuck.isEmpty()) {
            Timber.d("ForwardingDispatcher: no stuck records found")
            return
        }

        val delaySeconds = settingsRepository.settings.first().forwardingDelaySeconds.toLong()
        Timber.i("ForwardingDispatcher: re-enqueueing %d stuck record(s)", stuck.size)

        stuck.forEach { record ->
            workManager.enqueueUniqueWork(
                "forward_${record.id}",
                ExistingWorkPolicy.KEEP,
                buildRequest(record.id, record.sender, record.fullBody, delaySeconds),
            )
            Timber.i(
                "ForwardingDispatcher: re-enqueued stuck record id=%s sender=%s",
                record.id, record.sender,
            )
        }
    }

    private suspend fun enqueue(event: OtpEvent) {
        val delaySeconds = settingsRepository.settings.first().forwardingDelaySeconds.toLong()

        workManager.enqueueUniqueWork(
            "forward_${event.id}",
            ExistingWorkPolicy.KEEP,
            buildRequest(event.id, event.sender, event.fullBody, delaySeconds),
        )

        Timber.i(
            "ForwardingDispatcher: enqueued ForwardingWorker for event %s " +
                "(sender=%s, delay=%ds)",
            event.id, event.sender, delaySeconds,
        )

        devLogRepository.log(event.id, DevLogEntry(
            timestamp = Clock.System.now(),
            stage     = DevLogStage.WORKER_DISPATCHED,
            status    = DevLogStatus.OK,
            message   = "ForwardingWorker enqueued via WorkManager",
            detail    = "delay=${delaySeconds}s policy=KEEP uniqueWorkName=forward_${event.id}",
        ))
    }

    /**
     * Builds a [OneTimeWorkRequest] for [ForwardingWorker] with:
     * - sender + fullBody in the input data (avoids the Room-StateFlow race)
     * - exponential backoff (30 s base)
     * - expedited priority so OTPs run immediately on Samsung / battery-optimised devices
     */
    private fun buildRequest(
        id: String,
        sender: String,
        fullBody: String,
        delaySeconds: Long,
    ): OneTimeWorkRequest {
        val builder = OneTimeWorkRequestBuilder<ForwardingWorker>()
            .setInputData(
                workDataOf(
                    ForwardingWorker.KEY_EVENT_ID  to id,
                    ForwardingWorker.KEY_SENDER    to sender,
                    ForwardingWorker.KEY_FULL_BODY to fullBody,
                )
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)

        if (delaySeconds > 0) {
            builder.setInitialDelay(delaySeconds, TimeUnit.SECONDS)
        } else {
            // Run as an expedited job so OTPs are forwarded immediately even on
            // devices with aggressive battery optimisation (e.g. Samsung One UI).
            // Falls back to a regular work request if the app is out of quota.
            builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        }

        return builder.build()
    }

    /**
     * Manually forces a retry for a given record ID immediately, ignoring the global delay.
     */
    suspend fun forceRetry(eventId: String) {
        val record = forwardingRepository.records.first().find { it.id == eventId }
        if (record == null) {
            Timber.w("ForwardingDispatcher: Cannot force retry, record not found (id=%s)", eventId)
            return
        }

        workManager.enqueueUniqueWork(
            "forward_${record.id}",
            ExistingWorkPolicy.REPLACE, // Override any existing job to force it immediately
            buildRequest(record.id, record.sender, record.fullBody, delaySeconds = 0L),
        )

        devLogRepository.log(eventId, DevLogEntry(
            timestamp = Clock.System.now(),
            stage     = DevLogStage.WORKER_DISPATCHED,
            status    = DevLogStatus.OK,
            message   = "ForwardingWorker manually re-enqueued (force retry)",
            detail    = "policy=REPLACE delay=0s",
        ))

        Timber.i("ForwardingDispatcher: manually forced retry for id=%s", eventId)
    }
}
