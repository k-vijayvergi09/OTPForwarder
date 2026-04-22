package com.samsung.android.otpforwarder.core.domain

import com.samsung.android.otpforwarder.core.model.OtpEvent
import com.samsung.android.otpforwarder.core.model.SmsMessage

/**
 * Application-layer use case that wraps [OtpDetector].
 *
 * Callers (BroadcastReceiver → Worker, ViewModel tests) should depend on this
 * class rather than [OtpDetector] directly, keeping the domain boundary clean
 * and enabling future orchestration (allow/block lists, rate limiting, etc.)
 * without changing call sites.
 *
 * @param detector The underlying detection strategy injected by Hilt.
 */
class DetectOtpUseCase(private val detector: OtpDetector) {

    /**
     * @return [OtpEvent] if an OTP was found, `null` otherwise.
     */
    operator fun invoke(message: SmsMessage): OtpEvent? {
        return when (val result = detector.detect(message)) {
            is OtpDetectionResult.Detected -> result.event
            is OtpDetectionResult.NotOtp -> null
        }
    }
}
