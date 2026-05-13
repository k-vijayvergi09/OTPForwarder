package com.samsung.android.otpforwarder.core.domain

import com.samsung.android.otpforwarder.core.model.SmsDestination
import kotlinx.coroutines.flow.Flow

/**
 * Contract for reading and mutating [SmsDestination] data.
 *
 * Replaces the per-rule `RulesRepository` from the (now-removed) rules feature.
 * In this simplified model, every detected OTP fans out to every destination
 * with [SmsDestination.isEnabled] = true — there is no per-sender routing.
 *
 * Backed by Room ([core.database.RoomSmsDestinationRepository]).
 */
interface SmsDestinationRepository {

    /** Observe every SMS destination, including disabled ones (for the list UI). */
    fun observeAll(): Flow<List<SmsDestination>>

    /** Observe only enabled destinations — useful for "destinations watching" counts. */
    fun observeEnabled(): Flow<List<SmsDestination>>

    /**
     * One-shot read of the currently enabled destinations. Called by
     * [ForwardingWorker] each time a forwarding job runs, so it always picks up
     * additions/toggles the user has made since the job was enqueued.
     */
    suspend fun enabledOnce(): List<SmsDestination>

    /** Insert or update a destination (keyed by [SmsDestination.id]). */
    suspend fun upsert(destination: SmsDestination)

    /** Remove a destination by id. No-op if it does not exist. */
    suspend fun delete(id: String)

    /** Toggle the enabled flag without rewriting the rest of the row. */
    suspend fun setEnabled(id: String, enabled: Boolean)
}
