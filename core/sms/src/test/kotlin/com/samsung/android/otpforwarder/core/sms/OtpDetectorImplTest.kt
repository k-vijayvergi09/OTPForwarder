package com.samsung.android.otpforwarder.core.sms

import com.samsung.android.otpforwarder.core.domain.OtpDetectionResult
import com.samsung.android.otpforwarder.core.model.SmsMessage
import kotlinx.datetime.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class OtpDetectorImplTest {

    private lateinit var detector: OtpDetectorImpl

    @Before
    fun setUp() {
        detector = OtpDetectorImpl()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun sms(body: String, sender: String = "TEST-SENDER") = SmsMessage(
        id = UUID.randomUUID().toString(),
        sender = sender,
        body = body,
        receivedAt = Clock.System.now(),
    )

    private fun assertDetected(body: String, expectedCode: String) {
        val result = detector.detect(sms(body))
        assertTrue(
            "Expected Detected for body: \"$body\" but got $result",
            result is OtpDetectionResult.Detected,
        )
        assertEquals(
            "Wrong OTP code for body: \"$body\"",
            expectedCode,
            (result as OtpDetectionResult.Detected).event.otpCode,
        )
    }

    private fun assertNotOtp(body: String) {
        val result = detector.detect(sms(body))
        assertTrue(
            "Expected NotOtp for body: \"$body\" but got $result",
            result is OtpDetectionResult.NotOtp,
        )
    }

    // -------------------------------------------------------------------------
    // Standard OTP formats
    // -------------------------------------------------------------------------

    @Test fun `detects 6-digit OTP with keyword OTP`() {
        assertDetected("Your OTP is 123456. Do not share it.", "123456")
    }

    @Test fun `detects 4-digit PIN`() {
        assertDetected("Your PIN is 9821. Valid for 5 minutes.", "9821")
    }

    @Test fun `detects 8-digit code with 'code' keyword`() {
        assertDetected("Verification code: 87654321. Use within 10 mins.", "87654321")
    }

    @Test fun `detects OTP in lowercase keyword`() {
        assertDetected("otp: 554433 is your login code.", "554433")
    }

    @Test fun `detects OTP with 'one-time' keyword`() {
        assertDetected("Your one-time password is 112233.", "112233")
    }

    @Test fun `detects OTP with 'one time' (space) keyword`() {
        assertDetected("Use one time code 445566 to sign in.", "445566")
    }

    @Test fun `detects OTP with verification keyword`() {
        assertDetected("Verification code 778899 — expires in 3 min.", "778899")
    }

    @Test fun `detects OTP from bank-style message`() {
        assertDetected(
            "Dear Customer, use OTP 987654 to authorise your transaction. Do not share.",
            "987654",
        )
    }

    @Test fun `detects OTP when code appears before keyword`() {
        // Some senders put the code first.
        assertDetected("654321 is your verification code.", "654321")
    }

    @Test fun `detects OTP with auth code keyword`() {
        assertDetected("Authentication code: 246810", "246810")
    }

    @Test fun `detects OTP with passcode keyword`() {
        assertDetected("Your passcode is 135791.", "135791")
    }

    @Test fun `detects OTP with token keyword`() {
        assertDetected("Your token is 112358. It expires in 60 seconds.", "112358")
    }

    // -------------------------------------------------------------------------
    // Multiple candidate codes — pick the closest to the keyword
    // -------------------------------------------------------------------------

    @Test fun `picks code closest to keyword when multiple candidates present`() {
        // "OTP" is closer to 567890 than to 111222 (phone-like number at the end)
        assertDetected(
            "Call us at 9999999999. Your OTP is 567890.",
            "567890",
        )
    }

    @Test fun `ignores phone number embedded in body`() {
        assertDetected(
            "Hi, your OTP is 4321. For help call 1800123456.",
            "4321",
        )
    }

    // -------------------------------------------------------------------------
    // Edge cases — digit length
    // -------------------------------------------------------------------------

    @Test fun `rejects 3-digit number — too short`() {
        // "123" alone is < 4 digits and should not be treated as OTP.
        assertNotOtp("Your code is 123. Too short.")
    }

    @Test fun `rejects 9-digit number — too long`() {
        // 9 digits exceeds the 4-8 range.
        assertNotOtp("Your OTP is 123456789. Too long.")
    }

    @Test fun `accepts exactly 4 digits`() {
        assertDetected("OTP: 5678", "5678")
    }

    @Test fun `accepts exactly 8 digits`() {
        assertDetected("Your code is 12345678.", "12345678")
    }

    // -------------------------------------------------------------------------
    // Non-OTP messages — should return NotOtp
    // -------------------------------------------------------------------------

    @Test fun `ignores plain promotional SMS`() {
        assertNotOtp("Get 50% off on all items this weekend. Visit store now!")
    }

    @Test fun `ignores delivery notification`() {
        assertNotOtp("Your parcel has been dispatched. Expected delivery: Tomorrow.")
    }

    @Test fun `ignores blank body`() {
        assertNotOtp("")
    }

    @Test fun `ignores SMS with only digits and no keyword`() {
        // A 6-digit number alone, no keyword — should not trigger.
        assertNotOtp("123456")
    }

    // -------------------------------------------------------------------------
    // OtpEvent fields
    // -------------------------------------------------------------------------

    @Test fun `OtpEvent carries correct sender and body`() {
        val message = sms("Your OTP is 998877.", sender = "+919876543210")
        val result = detector.detect(message) as OtpDetectionResult.Detected
        assertEquals("+919876543210", result.event.sender)
        assertEquals("Your OTP is 998877.", result.event.fullBody)
        assertEquals(message.id, result.event.id)
    }

    @Test fun `OtpEvent sourceMessage matches input`() {
        val message = sms("OTP: 102030")
        val result = detector.detect(message) as OtpDetectionResult.Detected
        assertEquals(message, result.event.sourceMessage)
    }

    // -------------------------------------------------------------------------
    // Keyword regex coverage
    // -------------------------------------------------------------------------

    @Test fun `KEYWORD_REGEX matches OTP (case-insensitive)`() {
        assertTrue(OtpDetectorImpl.KEYWORD_REGEX.containsMatchIn("Your OTP is ready"))
        assertTrue(OtpDetectorImpl.KEYWORD_REGEX.containsMatchIn("your otp is ready"))
        assertTrue(OtpDetectorImpl.KEYWORD_REGEX.containsMatchIn("your Otp Is Ready"))
    }

    @Test fun `DIGIT_REGEX does not match 3-digit run`() {
        val matches = OtpDetectorImpl.DIGIT_REGEX.findAll("abc 123 def").toList()
        assertTrue(matches.isEmpty())
    }

    @Test fun `DIGIT_REGEX does not match 9-digit run`() {
        val matches = OtpDetectorImpl.DIGIT_REGEX.findAll("abc 123456789 def").toList()
        assertTrue(matches.isEmpty())
    }

    @Test fun `DIGIT_REGEX matches 4 through 8 digit runs`() {
        (4..8).forEach { len ->
            val digits = "1".repeat(len)
            val matches = OtpDetectorImpl.DIGIT_REGEX.findAll("code $digits end").toList()
            assertEquals("Expected 1 match for $len digits", 1, matches.size)
            assertEquals(digits, matches.first().value)
        }
    }
}
