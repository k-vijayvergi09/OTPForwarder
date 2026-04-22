package com.samsung.android.otpforwarder.core.domain

import com.samsung.android.otpforwarder.core.model.SmsMessage

/**
 * Contract for detecting an OTP inside an incoming [SmsMessage].
 *
 * Implementations are responsible for:
 *  - Matching OTP patterns (regex, ML, etc.)
 *  - Extracting the numeric code
 *  - Populating an [OtpDetectionResult.Detected] with a fully formed [com.samsung.android.otpforwarder.core.model.OtpEvent]
 *
 * This is a pure interface — no Android dependencies — so it can be unit-tested
 * without Robolectric.
 */
interface OtpDetector {
    /**
     * Analyse [message] and return a detection result.
     *
     * Always returns a non-null value; use the sealed type to branch on outcome.
     */
    fun detect(message: SmsMessage): OtpDetectionResult
}
