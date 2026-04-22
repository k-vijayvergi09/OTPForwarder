package com.samsung.android.otpforwarder.core.domain

import com.samsung.android.otpforwarder.core.model.OtpEvent

/**
 * Result of running OTP detection against a single [com.samsung.android.otpforwarder.core.model.SmsMessage].
 */
sealed class OtpDetectionResult {

    /**
     * An OTP was found in the message.
     *
     * @param event The fully populated [OtpEvent] ready for forwarding.
     */
    data class Detected(val event: OtpEvent) : OtpDetectionResult()

    /**
     * The message did not contain a recognisable OTP pattern.
     *
     * @param reason Human-readable diagnostic (used in debug logs only).
     */
    data class NotOtp(val reason: String = "No OTP pattern matched") : OtpDetectionResult()
}
