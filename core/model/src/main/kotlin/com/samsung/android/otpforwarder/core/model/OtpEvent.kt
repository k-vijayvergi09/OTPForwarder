package com.samsung.android.otpforwarder.core.model

import kotlinx.datetime.Instant

/**
 * A successfully detected OTP extracted from an [SmsMessage].
 *
 * @param id          Unique event ID (copied from [sourceMessage.id]).
 * @param otpCode     The extracted numeric OTP string (4–8 digits).
 * @param sender      Originating address of the SMS (convenience copy).
 * @param fullBody    Full SMS body for display / audit purposes.
 * @param detectedAt  When the OTP was extracted (usually same as receivedAt).
 * @param sourceMessage The original [SmsMessage] that contained this OTP.
 */
data class OtpEvent(
    val id: String,
    val otpCode: String,
    val sender: String,
    val fullBody: String,
    val detectedAt: Instant,
    val sourceMessage: SmsMessage,
)
