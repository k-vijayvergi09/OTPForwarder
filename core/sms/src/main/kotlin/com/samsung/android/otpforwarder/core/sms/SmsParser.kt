package com.samsung.android.otpforwarder.core.sms

import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage as AndroidSmsMessage
import com.samsung.android.otpforwarder.core.model.SmsMessage
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * Parses a raw [android.content.Intent.ACTION_SMS_RECEIVED] broadcast into a
 * list of [SmsMessage] domain objects.
 *
 * One broadcast can carry multiple PDUs from the same sender (multi-part SMS),
 * and multi-part messages are merged into a single [SmsMessage] so that the
 * OTP detector sees the complete text.
 */
class SmsParser @Inject constructor() {

    /**
     * Extract [SmsMessage] objects from the broadcast [intent].
     *
     * Returns an empty list when:
     * - The intent extras do not contain PDU data
     * - All PDUs fail to parse
     */
    fun parse(intent: Intent): List<SmsMessage> {
        return try {
            // API 19+ helper reads the PDU array and subscription ID for us.
            val rawMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (rawMessages.isNullOrEmpty()) {
                Timber.w("SmsParser: intent contained no PDUs")
                return emptyList()
            }

            // Group multi-part PDUs by sender so each logical message is merged.
            rawMessages
                .groupBy { it.originatingAddress.orEmpty() }
                .mapNotNull { (sender, parts) ->
                    mergePartsOrNull(sender, parts)
                }
        } catch (e: Exception) {
            Timber.e(e, "SmsParser: failed to parse SMS intent")
            emptyList()
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun mergePartsOrNull(
        sender: String,
        parts: List<AndroidSmsMessage>,
    ): SmsMessage? {
        if (parts.isEmpty()) return null

        val body = parts.joinToString(separator = "") { it.messageBody.orEmpty() }
        if (body.isBlank()) return null

        // Use the timestamp from the first PDU; fall back to system clock.
        val timestampMs = parts.first().timestampMillis
        val receivedAt: Instant = if (timestampMs > 0L) {
            Instant.fromEpochMilliseconds(timestampMs)
        } else {
            Clock.System.now()
        }

        val subscriptionId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // subId is only available from API 29 via getMessagesFromIntent
            // The subId is embedded in the extras; best-effort extraction.
            parts.first().let { msg ->
                // Reflection-free: subId is not exposed pre-29 in a stable API.
                // We rely on the intent extra parsed by Telephony.Sms.Intents above.
                -1 // Will be enriched by SmsReceiver when API >= 29.
            }
        } else {
            -1
        }

        return SmsMessage(
            id = UUID.randomUUID().toString(),
            sender = sender,
            body = body,
            receivedAt = receivedAt,
            subscriptionId = subscriptionId,
        )
    }
}
