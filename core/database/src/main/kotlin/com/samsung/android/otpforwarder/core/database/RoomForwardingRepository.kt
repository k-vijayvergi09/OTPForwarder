package com.samsung.android.otpforwarder.core.database

import com.samsung.android.otpforwarder.core.common.coroutines.ApplicationScope
import com.samsung.android.otpforwarder.core.database.dao.ForwardingRecordDao
import com.samsung.android.otpforwarder.core.database.entity.ForwardingRecordEntity
import com.samsung.android.otpforwarder.core.database.entity.toDomain
import com.samsung.android.otpforwarder.core.database.entity.toEntity
import com.samsung.android.otpforwarder.core.domain.ForwardingRepository
import com.samsung.android.otpforwarder.core.model.DestinationType
import com.samsung.android.otpforwarder.core.model.ForwardingRecord
import com.samsung.android.otpforwarder.core.model.ForwardingStatus
import com.samsung.android.otpforwarder.core.model.OtpEvent
import com.samsung.android.otpforwarder.core.model.TodayStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ForwardingRepository] backed by Room.
 *
 * [records] is converted from a Room [Flow] to a [StateFlow] using [SharingStarted.Eagerly]
 * so the first collector always gets the current DB snapshot immediately — matching the
 * contract of the in-memory predecessor.
 *
 * [recordDetectedOtp] is intentionally non-suspending (called from [BroadcastReceiver.onReceive])
 * and fire-and-forgets a coroutine on [ApplicationScope].
 */
@Singleton
class RoomForwardingRepository @Inject constructor(
    private val dao: ForwardingRecordDao,
    @ApplicationScope private val scope: CoroutineScope,
) : ForwardingRepository {

    override val records: StateFlow<List<ForwardingRecord>> = dao
        .observeAll()
        .map { entities -> entities.map(ForwardingRecordEntity::toDomain) }
        .stateIn(
            scope   = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    // Non-suspending — safe to call from BroadcastReceiver.onReceive.
    override fun recordDetectedOtp(event: OtpEvent) {
        val record = ForwardingRecord(
            id         = event.id,
            otpCode    = event.otpCode,
            sender     = event.sender,
            fullBody   = event.fullBody,
            status     = ForwardingStatus.PENDING,
            receivedAt = event.detectedAt,
            updatedAt  = Clock.System.now(),
        )
        scope.launch { dao.upsert(record.toEntity()) }
    }

    override fun todayRecords(): Flow<List<ForwardingRecord>> = records.map { list ->
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        list.filter { record ->
            record.receivedAt
                .toLocalDateTime(TimeZone.currentSystemDefault()).date == today
        }
    }

    override fun todayStats(): Flow<TodayStats> = todayRecords().map { list ->
        TodayStats(
            forwarded = list.count { it.status == ForwardingStatus.FORWARDED },
            failed    = list.count { it.status == ForwardingStatus.FAILED },
            pending   = list.count {
                it.status == ForwardingStatus.PENDING ||
                it.status == ForwardingStatus.RETRY_QUEUED
            },
        )
    }

    override suspend fun addRecord(record: ForwardingRecord) {
        dao.upsert(record.toEntity())
    }

    override suspend fun updateStatus(id: String, status: ForwardingStatus, error: String?) {
        dao.updateStatus(
            id           = id,
            status       = status.name,
            errorMessage = error,
            updatedAt    = Clock.System.now().toEpochMilliseconds(),
        )
    }

    /**
     * Convenience method used by [ForwardingWorker] to record which channels
     * a record was successfully delivered through, without touching the status.
     */
    override suspend fun updateDestinations(id: String, destinations: List<DestinationType>) {
        dao.updateDestinations(
            id           = id,
            destinations = destinations.joinToString(",") { it.name },
            updatedAt    = Clock.System.now().toEpochMilliseconds(),
        )
    }

    override suspend fun pendingRecords(): List<ForwardingRecord> =
        dao.getByStatuses(
            listOf(ForwardingStatus.PENDING.name, ForwardingStatus.RETRY_QUEUED.name)
        ).map(ForwardingRecordEntity::toDomain)
}
