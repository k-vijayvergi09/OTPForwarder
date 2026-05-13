package com.samsung.android.otpforwarder.core.domain

import com.samsung.android.otpforwarder.core.model.EmailDestination
import kotlinx.coroutines.flow.Flow

/**
 * Contract for reading and mutating [EmailDestination] data.
 *
 * Mirrors [SmsDestinationRepository] but for email. Email forwarding itself is
 * scheduled for milestone M3 — until then this repository is used only by the
 * Destinations UI for CRUD; [ForwardingWorker] does not yet consume from it.
 */
interface EmailDestinationRepository {

    fun observeAll(): Flow<List<EmailDestination>>

    fun observeEnabled(): Flow<List<EmailDestination>>

    /** One-shot read of currently enabled destinations (for the future email worker). */
    suspend fun enabledOnce(): List<EmailDestination>

    suspend fun upsert(destination: EmailDestination)

    suspend fun delete(id: String)

    suspend fun setEnabled(id: String, enabled: Boolean)
}
