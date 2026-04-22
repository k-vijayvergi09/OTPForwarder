package com.samsung.android.otpforwarder.core.data

import com.samsung.android.otpforwarder.core.domain.ForwardingRepository
import com.samsung.android.otpforwarder.core.model.ForwardingRecord
import com.samsung.android.otpforwarder.core.model.ForwardingStatus
import com.samsung.android.otpforwarder.core.model.OtpEvent
import com.samsung.android.otpforwarder.core.model.TodayStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory [ForwardingRepository] for M1.
 *
 * Stores detected OTP events as [ForwardingRecord]s with
 * [ForwardingStatus.PENDING] status.
 * Records are lost when the process is killed — Room-backed persistence comes in M4.
 */
@Singleton
class InMemoryForwardingRepository @Inject constructor() : ForwardingRepository {

    private val _records = MutableStateFlow<List<ForwardingRecord>>(emptyList())
    override val records: StateFlow<List<ForwardingRecord>> = _records.asStateFlow()

    override fun recordDetectedOtp(event: OtpEvent) {
        val record = ForwardingRecord(
            id = event.id,
            otpCode = event.otpCode,
            sender = event.sender,
            fullBody = event.fullBody,
            status = ForwardingStatus.PENDING,
            receivedAt = event.detectedAt,
            updatedAt = Clock.System.now(),
        )
        _records.update { current ->
            listOf(record) + current.filter { it.id != record.id }
        }
    }

    override fun todayRecords(): Flow<List<ForwardingRecord>> = records.map { list ->
        val todayDate = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        list.filter { record ->
            record.receivedAt
                .toLocalDateTime(TimeZone.currentSystemDefault()).date == todayDate
        }
    }

    override fun todayStats(): Flow<TodayStats> = todayRecords().map { list ->
        TodayStats(
            forwarded = list.count { it.status == ForwardingStatus.FORWARDED },
            failed    = list.count { it.status == ForwardingStatus.FAILED },
            pending   = list.count {
                it.status == ForwardingStatus.PENDING || it.status == ForwardingStatus.RETRY_QUEUED
            },
        )
    }

    override suspend fun addRecord(record: ForwardingRecord) {
        _records.update { current ->
            // Prepend so newest is first; deduplicate by id just in case.
            listOf(record) + current.filter { it.id != record.id }
        }
    }

    override suspend fun updateStatus(id: String, status: ForwardingStatus, error: String?) {
        _records.update { current ->
            current.map { record ->
                if (record.id == id) {
                    record.copy(
                        status       = status,
                        errorMessage = error,
                        updatedAt    = Clock.System.now(),
                    )
                } else {
                    record
                }
            }
        }
    }
}
