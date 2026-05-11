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
 * 1. The message body must pass a two-tier keyword gate:
 *    - Tier 1: standalone words that are unambiguous OTP signals on their own
 *      ("otp", "password", "pin"). A single hit is enough to proceed.
 *    - Tier 2: contextual phrases that are strong signals but only meaningful
 *      together ("verification code", "use code", etc.). Consulted only when
 *      no Tier-1 word is found. Deliberately avoids bare "code", "token", or
 *      "expires" which appear in too many non-OTP messages (tracking updates,
 *      promo emails, etc.).
 * 2. A 4–8 digit numeric code must appear in the body, surrounded by
 *    word boundaries so partial matches inside longer numbers are excluded.
 * 3. When multiple candidate codes exist the one closest to the matched
 *    keyword is preferred; ties are broken by taking the first.
 *
 * This covers the vast majority of real-world OTP formats observed in the
 * wild while keeping false-positive rates very low.  A future ML classifier
 * can replace or augment this without changing any call sites.
 */
class OtpDetectorImpl @Inject constructor() : OtpDetector {

    override fun detect(message: SmsMessage): OtpDetectionResult {
        val body = message.body

        // Step 1 — two-tier keyword gate (fast path rejection).
        // Prefer the Tier-1 match as the digit-proximity anchor when both tiers
        // match, since Tier-1 words tend to sit right next to the code.
        val keywordMatch = TIER1_KEYWORD_REGEX.find(body)
            ?: TIER2_KEYWORD_REGEX.find(body)
            ?: return OtpDetectionResult.NotOtp("No OTP keyword found in body")

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
         * Tier-1: standalone words that are unambiguous OTP signals on their own.
         * A single match is sufficient to pass the keyword gate.
         *
         * - "otp"      — explicit abbreviation, virtually never appears outside OTP messages.
         * - "password" — one-time passwords, temporary passwords from banks / services.
         * - "pin"      — ATM PINs, app PINs, device PINs sent via SMS.
         */
        val TIER1_KEYWORD_REGEX: Regex = Regex(
            pattern = """\b(otp|password|pin)\b""",
            options = setOf(RegexOption.IGNORE_CASE),
        )

        /**
         * Tier-2: contextual phrases that together strongly indicate an OTP message.
         * Only consulted when no Tier-1 word is present.
         *
         * Deliberately excludes bare "code", "token", "verify", and "expires" —
         * these appear too frequently in shipping notifications, promo emails,
         * and other non-OTP messages to be useful on their own.
         */
        val TIER2_KEYWORD_REGEX: Regex = Regex(
            pattern = """\b(one[- ]time|verification\s+code|use\s+code|"""
                + """auth(?:entication)?\s+code|passcode|secret\s+code)\b""",
            options = setOf(RegexOption.IGNORE_CASE),
        )

        /**
         * Matches standalone 4–8 digit sequences.
         * Word boundary `\b` prevents matching inside longer numbers (e.g. phone numbers).
         */
        val DIGIT_REGEX: Regex = Regex("""\b\d{4,8}\b""")
    }
}
