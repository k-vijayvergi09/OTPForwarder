package com.samsung.android.otpforwarder.core.domain

import com.samsung.android.otpforwarder.core.model.DevLog
import com.samsung.android.otpforwarder.core.model.DevLogEntry
import kotlinx.coroutines.flow.StateFlow

/**
 * Stores per-event developer debug logs produced by the SMS-to-forwarding pipeline.
 *
 * Implementations must be thread-safe. All write operations ([log]) are
 * non-suspending so they can be called from [BroadcastReceiver.onReceive]
 * and from WorkManager worker threads without ceremony.
 *
 * Logs are held in-memory only and are not persisted across process death —
 * they exist solely to help a developer who has Dev Mode enabled trace
 * exactly what happened to a given OTP event in real time.
 */
interface DevLogRepository {

    /**
     * Append a [DevLogEntry] for the given [eventId].
     *
     * Safe to call from any thread. Emits an updated value on [logs].
     */
    fun log(eventId: String, entry: DevLogEntry)

    /**
     * Snapshot of the accumulated [DevLog] for [eventId], or null if
     * no entries have been written for it yet.
     */
    fun getLog(eventId: String): DevLog?

    /**
     * Hot [StateFlow] of all logs keyed by eventId, newest entries last within
     * each [DevLog.entries] list. Emits on every [log] call.
     */
    val logs: StateFlow<Map<String, DevLog>>

    /**
     * Remove all entries for [eventId] to free memory.
     * No-op if the event has no log.
     */
    fun clearLog(eventId: String)
}
