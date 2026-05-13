package com.samsung.android.otpforwarder.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.samsung.android.otpforwarder.core.model.EmailDestination

/**
 * Room entity mirroring [EmailDestination].
 *
 * One row per user-configured email forwarding destination.
 */
@Entity(tableName = "email_destinations")
data class EmailDestinationEntity(
    @PrimaryKey
    val id: String,
    val label: String,
    val emailAddress: String,
    val isEnabled: Boolean,
)

// ── Mappers ───────────────────────────────────────────────────────────────────

fun EmailDestinationEntity.toDomain(): EmailDestination = EmailDestination(
    id           = id,
    label        = label,
    emailAddress = emailAddress,
    isEnabled    = isEnabled,
)

fun EmailDestination.toEntity(): EmailDestinationEntity = EmailDestinationEntity(
    id           = id,
    label        = label,
    emailAddress = emailAddress,
    isEnabled    = isEnabled,
)
