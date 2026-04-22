package com.samsung.android.otpforwarder.core.sms

import com.samsung.android.otpforwarder.core.domain.OtpDetectionResult
import com.samsung.android.otpforwarder.core.domain.OtpDetector
import com.samsung.android.otpforwarder.core.model.OtpEvent
import com.samsung.android.otpforwarder.core.model.SmsMessage
import kotlinx.datetime.Clock
import timber.log.Timber
import javax.inject.Inject

/**
 * Regex-based [OtpDetector] implementation.
 *
 * Detection strategy (applied in order):
 * 1. The message body must contain at least one OTP-related keyword.
 * 2. A 4–8 digit numeric code must appear in the body, surrounded by
 *    word boundaries so partial matches inside longer numbers are excluded.
 * 3. When multiple candidate codes exist the one closest to the first
 *    keyword occurrence is preferred; ties are broken by taking the first.
 *
 * This covers the vast majority of real-world OTP formats observed in the
 * wild while keeping false-positive rates very low.  A future ML classifier
 * can replace or augment this without changing any call sites.
 */
class OtpDetectorImpl @Inject constructor() : OtpDetector {

    override fun detect(message: SmsMessage): OtpDetectionResult {
        val body = message.body

        // Step 1 — keyword gate (fast path rejection).
        val keywordMatch = KEYWORD_REGEX.find(body)
        if (keywordMatch == null) {
            return OtpDetectionResult.NotOtp("No OTP keyword found in body")
        }

        // Step 2 — find all candidate digit sequences (4–8 digits).
        val candidates = DIGIT_REGEX.findAll(body).toList()
        if (candidates.isEmpty()) {
            return OtpDetectionResult.NotOtp("Keyword present but no 4–8 digit code found")
        }

        // Step 3 — pick the candidate closest to the keyword.
        val keywordCenter = keywordMatch.range.first
        val best = candidates.minByOrNull { kotlin.math.abs(it.range.first - keywordCenter) }
            ?: candidates.first()

        val otpCode = best.value
        Timber.d("OtpDetectorImpl: detected OTP=%s from sender=%s", otpCode, message.sender)

        val event = OtpEvent(
            id = message.id,
            otpCode = otpCode,
            sender = message.sender,
            fullBody = body,
            detectedAt = Clock.System.now(),
            sourceMessage = message,
        )
        return OtpDetectionResult.Detected(event)
    }

    // -------------------------------------------------------------------------
    // Companion — regex constants
    // -------------------------------------------------------------------------

    companion object {
        /**
         * Matches common OTP-related keywords, case-insensitive.
         * Covers English terms; extend this list for regional languages in v1.1.
         */
        val KEYWORD_REGEX: Regex = Regex(
            pattern = """(?i)\b(otp|one[- ]time|verification\s+code|verify|verif[yi]|"""
                + """auth(?:entication)?\s+code|passcode|password|pin|secret\s+code|"""
                + """code|token|expires?)\b""",
            options = setOf(RegexOption.IGNORE_CASE),
        )

        /**
         * Matches standalone 4–8 digit sequences.
         * Word boundary `\b` prevents matching inside longer numbers (e.g. phone numbers).
         */
        val DIGIT_REGEX: Regex = Regex("""\b\d{4,8}\b""")
    }
}
