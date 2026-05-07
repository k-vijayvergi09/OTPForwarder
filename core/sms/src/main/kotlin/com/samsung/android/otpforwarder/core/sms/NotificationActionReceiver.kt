package com.samsung.android.otpforwarder.core.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.samsung.android.otpforwarder.core.common.coroutines.ApplicationScope
import com.samsung.android.otpforwarder.core.domain.ForwardingRepository
import com.samsung.android.otpforwarder.core.model.ForwardingStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Handles actions dispatched from notification buttons (e.g., "Retry").
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var forwardingDispatcher: ForwardingDispatcher

    @Inject
    lateinit var forwardingRepository: ForwardingRepository

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_RETRY_FORWARDING) {
            val eventId = intent.getStringExtra(EXTRA_EVENT_ID)
            if (eventId == null) {
                Timber.e("NotificationActionReceiver: No event ID provided for retry")
                return
            }

            Timber.i("NotificationActionReceiver: Retry requested for event %s", eventId)
            
            // Dispatch the work asynchronously using the application scope
            // because BroadcastReceiver's execution window is short.
            scope.launch {
                forwardingRepository.updateStatus(eventId, ForwardingStatus.PENDING, null)
                forwardingDispatcher.forceRetry(eventId)
            }
        }
    }

    companion object {
        const val ACTION_RETRY_FORWARDING = "com.samsung.android.otpforwarder.action.RETRY_FORWARDING"
        const val EXTRA_EVENT_ID = "extra_event_id"
    }
}
