package com.samsung.android.otpforwarder.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.samsung.android.otpforwarder.core.model.DestinationType
import com.samsung.android.otpforwarder.core.model.ForwardingRecord
import com.samsung.android.otpforwarder.core.model.ForwardingStatus
import kotlinx.datetime.Instant

/**
 * Room entity mirroring [ForwardingRecord].
 *
 * Type mappings:
 * - [ForwardingStatus] → stored as its [Enum.name] string.
 * - [List<DestinationType>] → comma-separated names (e.g. "SMS,EMAIL").
 *   Unknown names are silently dropped on read for forward-compatibility.
 * - [Instant] → [Long] epoch milliseconds.
 */
@Entity(tableName = "forwarding_records")
data class ForwardingRecordEntity(
    @PrimaryKey
    val id: String,
    val otpCode: String,
    val sender: String,
    val fullBody: String,
    /** [ForwardingStatus.name] */
    val status: String,
    /** Comma-separated [DestinationType.name] values, e.g. "SMS,EMAIL". Empty string = no destinations. */
    val destinations: String,
    val errorMessage: String?,
    val retryCount: Int,
    /** [Instant.toEpochMilliseconds] */
    val receivedAt: Long,
    /** [Instant.toEpochMilliseconds] */
    val updatedAt: Long,
)

// ── Mappers ───────────────────────────────────────────────────────────────────

fun ForwardingRecordEntity.toDomain(): ForwardingRecord = ForwardingRecord(
    id           = id,
    otpCode      = otpCode,
    sender       = sender,
    fullBody     = fullBody,
    status       = status.toForwardingStatus(),
    destinations = destinations.toDestinationTypes(),
    errorMessage = errorMessage,
    retryCount   = retryCount,
    receivedAt   = Instant.fromEpochMilliseconds(receivedAt),
    updatedAt    = Instant.fromEpochMilliseconds(updatedAt),
)

fun ForwardingRecord.toEntity(): ForwardingRecordEntity = ForwardingRecordEntity(
    id           = id,
    otpCode      = otpCode,
    sender       = sender,
    fullBody     = fullBody,
    status       = status.name,
    destinations = destinations.toDestinationsString(),
    errorMessage = errorMessage,
    retryCount   = retryCount,
    receivedAt   = receivedAt.toEpochMilliseconds(),
    updatedAt    = updatedAt.toEpochMilliseconds(),
)

// ── Private helpers ───────────────────────────────────────────────────────────

private fun String.toForwardingStatus(): ForwardingStatus =
    try { ForwardingStatus.valueOf(this) } catch (_: IllegalArgumentException) { ForwardingStatus.FAILED }

private fun String.toDestinationTypes(): List<DestinationType> =
    if (isBlank()) emptyList()
    else split(",").mapNotNull { token ->
        try { DestinationType.valueOf(token.trim()) }
        catch (_: IllegalArgumentException) { null }
    }

private fun List<DestinationType>.toDestinationsString(): String =
    joinToString(",") { it.name }
