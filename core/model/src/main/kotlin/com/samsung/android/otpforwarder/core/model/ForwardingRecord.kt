package com.samsung.android.otpforwarder.core.model

import kotlinx.datetime.Instant

/**
 * A single OTP detection + forwarding attempt, persisted (initially in-memory,
 * later in Room). This is the canonical domain entity shown in Logs and Home.
 *
 * @param id           Stable ID — copied from [OtpEvent.id].
 * @param otpCode      The raw numeric code (used for masking logic in UI).
 * @param sender       Originating SMS address.
 * @param fullBody     Full SMS body for the detail screen.
 * @param status       Current [ForwardingStatus] of this record.
 * @param destinations The channels via which forwarding was attempted or succeeded.
 * @param errorMessage Human-readable error for [ForwardingStatus.FAILED] records.
 * @param retryCount   Number of retry attempts made so far.
 * @param receivedAt   When the OTP was detected.
 * @param updatedAt    When the status last changed.
 */
data class ForwardingRecord(
    val id: String,
    val otpCode: String,
    val sender: String,
    val fullBody: String,
    val status: ForwardingStatus,
    val destinations: List<DestinationType> = emptyList(),
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val receivedAt: Instant,
    val updatedAt: Instant,
)
