package com.samsung.android.otpforwarder.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.samsung.android.otpforwarder.core.model.SmsDestination

/**
 * Room entity mirroring [SmsDestination].
 *
 * One row per user-configured SMS forwarding destination. Phone numbers are
 * stored verbatim (no normalisation) — validation lives in the domain layer.
 */
@Entity(tableName = "sms_destinations")
data class SmsDestinationEntity(
    @PrimaryKey
    val id: String,
    val label: String,
    val phoneNumber: String,
    val isEnabled: Boolean,
)

// ── Mappers ───────────────────────────────────────────────────────────────────

fun SmsDestinationEntity.toDomain(): SmsDestination = SmsDestination(
    id          = id,
    label       = label,
    phoneNumber = phoneNumber,
    isEnabled   = isEnabled,
)

fun SmsDestination.toEntity(): SmsDestinationEntity = SmsDestinationEntity(
    id          = id,
    label       = label,
    phoneNumber = phoneNumber,
    isEnabled   = isEnabled,
)
