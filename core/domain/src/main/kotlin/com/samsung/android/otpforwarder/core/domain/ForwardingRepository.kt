package com.samsung.android.otpforwarder.core.domain

import com.samsung.android.otpforwarder.core.model.ForwardingRecord
import com.samsung.android.otpforwarder.core.model.ForwardingStatus
import com.samsung.android.otpforwarder.core.model.OtpEvent
import com.samsung.android.otpforwarder.core.model.TodayStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for reading and mutating [ForwardingRecord] data.
 *
 * In M1 the implementation is in-memory.
 * In M4 it will be backed by Room — the interface stays stable.
 */
interface ForwardingRepository {

    /**
     * Record a detected OTP immediately when it is observed from the SMS
     * broadcast path so the app can react even on a cold start.
     */
    fun recordDetectedOtp(event: OtpEvent)

    /** All records, newest first. Hot [StateFlow] — emits immediately on collection. */
    val records: StateFlow<List<ForwardingRecord>>

    /** Today's records only, newest first. Useful for the Home screen. */
    fun todayRecords(): Flow<List<ForwardingRecord>>

    /** Aggregated counts for today's activity (forwarded / failed / pending). */
    fun todayStats(): Flow<TodayStats>

    /** Persist a new record (initial status is typically [ForwardingStatus.PENDING]). */
    suspend fun addRecord(record: ForwardingRecord)

    /** Update the status (and optionally an error message) for an existing record. */
    suspend fun updateStatus(id: String, status: ForwardingStatus, error: String? = null)
}
