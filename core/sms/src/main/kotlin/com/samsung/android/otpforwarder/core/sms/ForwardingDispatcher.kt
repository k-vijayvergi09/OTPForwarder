package com.samsung.android.otpforwarder.core.sms

import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.samsung.android.otpforwarder.core.common.coroutines.ApplicationScope
import com.samsung.android.otpforwarder.core.domain.SettingsRepository
import com.samsung.android.otpforwarder.core.model.OtpEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
 * For each [OtpEvent] emitted by [SmsEventBus]:
 *   1. Reads the current [forwardingDelaySeconds] from settings.
 *   2. Enqueues a [ForwardingWorker] as unique work keyed by `event.id`
 *      ([ExistingWorkPolicy.KEEP]) to prevent duplicate delivery on re-broadcast.
 *   3. Applies exponential backoff so transient failures (e.g. no SEND_SMS
 *      permission yet) are retried up to WorkManager's default attempt limit.
 */
@Singleton
class ForwardingDispatcher @Inject constructor(
    private val eventBus: SmsEventBus,
    private val workManager: WorkManager,
    private val settingsRepository: SettingsRepository,
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
            Timber.d("ForwardingDispatcher: started, collecting OTP events")
            eventBus.events.collect { event ->
                enqueue(event)
            }
        }
    }

    private suspend fun enqueue(event: OtpEvent) {
        val delaySeconds = settingsRepository.settings.first().forwardingDelaySeconds.toLong()

        val request = OneTimeWorkRequestBuilder<ForwardingWorker>()
            .setInputData(workDataOf(ForwardingWorker.KEY_EVENT_ID to event.id))
            .apply {
                if (delaySeconds > 0) {
                    setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                }
            }
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            "forward_${event.id}",
            ExistingWorkPolicy.KEEP,
            request,
        )

        Timber.i(
            "ForwardingDispatcher: enqueued ForwardingWorker for event %s " +
                "(sender=%s, delay=%ds)",
            event.id, event.sender, delaySeconds,
        )
    }
}
