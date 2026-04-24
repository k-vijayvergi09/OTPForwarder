package com.samsung.android.otpforwarder

import android.content.Intent
import android.provider.Telephony

/**
 * Test-only helpers for constructing valid SMS broadcast intents.
 *
 * The real [android.provider.Telephony.Sms.Intents.getMessagesFromIntent] function
 * expects an intent that carries raw GSM PDU bytes in the "pdus" extra.  Passing
 * invalid bytes causes it to return null/empty, so [SmsParser] would silently
 * drop the message and the test would see nothing in the repository.
 *
 * [buildGsmDeliverPdu] creates a minimal but fully valid 3GPP SMS-DELIVER PDU
 * using GSM 7-bit alphabet encoding.  It supports ASCII letters, digits, space,
 * and common punctuation — everything needed for OTP test messages.
 */
object SmsTestPduUtils {

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Build an [Intent] with [Telephony.Sms.Intents.SMS_RECEIVED_ACTION] and
     * correct PDU/format extras.  Pass directly to [SmsReceiver.onReceive].
     *
     * @param sender  Phone number (e.g. "+15555215554") or short-code (e.g. "BankOTP").
     *                Only numeric senders are supported by this helper; use a numeric
     *                string like "12345" for short-codes.
     * @param body    Message text — ASCII letters/digits/space/punctuation only.
     */
    fun buildSmsReceivedIntent(sender: String, body: String): Intent =
        Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
            // "format" tells SmsMessage.createFromPdu which codec to use.
            // "3gpp" = GSM/UMTS/LTE; "3gpp2" = CDMA (not used here).
            putExtra("format", "3gpp")
            // "pdus" is read by Telephony.Sms.Intents.getMessagesFromIntent as Object[].
            // Each element is a byte[] representing one PDU (one part of a message).
            @Suppress("UNCHECKED_CAST")
            putExtra("pdus", arrayOf<Any>(buildGsmDeliverPdu(sender, body)))
        }

    // ── PDU construction ───────────────────────────────────────────────────────

    /**
     * Build a minimal valid 3GPP SMS-DELIVER PDU.
     *
     * Structure (3GPP TS 23.040 §9.2.2.1):
     * ```
     * [SMSC info (1 byte)]  00 = no SMSC address
     * [First octet]         04 = SMS-DELIVER, no flags set
     * [Originating Address] length + TOA + semi-octet-swapped digits
     * [Protocol Identifier] 00
     * [Data Coding Scheme]  00 = GSM 7-bit default alphabet
     * [SCTS]                7 bytes reversed-BCD timestamp
     * [UDL]                 number of septets (= body.length for 7-bit)
     * [UD]                  GSM 7-bit packed user data
     * ```
     */
    fun buildGsmDeliverPdu(sender: String, body: String): ByteArray {
        val out = mutableListOf<Int>()

        // ── SMSC info ──────────────────────────────────────────────────────────
        out += 0x00            // length = 0 means no SMSC address in PDU

        // ── First octet ────────────────────────────────────────────────────────
        // Bit 7 = RP=0 (no reply path), Bit 6 = UDHI=0 (no UDH), Bit 5 = SRI=0,
        // Bit 4-3 = reserved/MMS, Bits 1-0 = MTI=00 (SMS-DELIVER) → 0x04
        out += 0x04

        // ── Originating Address ────────────────────────────────────────────────
        val isInternational = sender.startsWith("+")
        val digits = sender.filter { it.isDigit() }
        out += digits.length                                   // digit count (NOT byte count)
        out += if (isInternational) 0x91 else 0x81             // TOA: 0x91=intl, 0x81=national

        // GSM semi-octet encoding: each byte holds two digits with nibbles swapped.
        // Odd-length strings are right-padded with 'F' (0xF).
        val padded = if (digits.length % 2 == 0) digits else "${digits}F"
        padded.chunked(2).forEach { pair ->
            val lo = pair[0].digitToInt()                      // first digit → low nibble
            val hi = if (pair[1] == 'F') 0xF else pair[1].digitToInt()
            out += (hi shl 4) or lo
        }

        // ── Protocol Identifier ────────────────────────────────────────────────
        out += 0x00            // 0x00 = Short Message Service (plain SMS)

        // ── Data Coding Scheme ─────────────────────────────────────────────────
        out += 0x00            // 0x00 = GSM 7-bit default alphabet, class unspecified

        // ── Service Centre Time Stamp (SCTS) ───────────────────────────────────
        // 7 bytes of reversed-BCD: year, month, day, hour, minute, second, timezone.
        // Fixed to 2026-04-22 12:00:00 UTC+00 for deterministic test output.
        //  year=26 → 0x62  month=04 → 0x40  day=22 → 0x22  hour=12 → 0x21
        //  min=00  → 0x00  sec=00  → 0x00   tz=+0 → 0x00
        out += listOf(0x62, 0x40, 0x22, 0x21, 0x00, 0x00, 0x00)

        // ── User Data ──────────────────────────────────────────────────────────
        out += body.length     // UDL = septet count (= char count for 7-bit, no UDH)
        out.addAll(pack7bit(body).map { it.toInt() and 0xFF })

        return out.map { it.toByte() }.toByteArray()
    }

    // ── GSM 7-bit packing ──────────────────────────────────────────────────────

    /**
     * Pack [text] into GSM 7-bit format.
     *
     * For ASCII letters (A-Z, a-z), digits (0-9), space, and common punctuation
     * the GSM 7-bit code point equals the ASCII code point — no translation needed
     * for typical OTP message bodies.
     *
     * Each septet (7 bits) is packed LSB-first into the output byte stream:
     *   septet[0] fills bits [0..6] of byte[0]
     *   septet[1] fills bit [7] of byte[0] and bits [0..5] of byte[1]
     *   septet[2] fills bits [6..7] of byte[1] and bits [0..4] of byte[2]
     *   … and so on.
     */
    private fun pack7bit(text: String): ByteArray {
        val numBytes = (text.length * 7 + 7) / 8
        val bytes = ByteArray(numBytes)

        text.forEachIndexed { i, char ->
            val septet   = char.code and 0x7F   // GSM 7-bit value (= ASCII for our test chars)
            val bitOffset = i * 7
            val byteIdx  = bitOffset / 8
            val bitShift = bitOffset % 8

            // Write lower (8 - bitShift) bits of septet into bytes[byteIdx]
            bytes[byteIdx] = (bytes[byteIdx].toInt() or (septet shl bitShift)).toByte()

            // If the septet straddles a byte boundary, write the remaining bits
            // into bytes[byteIdx + 1].  This happens whenever bitShift >= 2
            // (bitShift=0 and bitShift=1 both fit within the current byte).
            if (bitShift > 1 && byteIdx + 1 < numBytes) {
                bytes[byteIdx + 1] = (bytes[byteIdx + 1].toInt() or (septet ushr (8 - bitShift))).toByte()
            }
        }
        return bytes
    }
}
