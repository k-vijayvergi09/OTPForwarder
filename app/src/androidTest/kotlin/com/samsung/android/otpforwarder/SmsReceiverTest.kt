package com.samsung.android.otpforwarder

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.samsung.android.otpforwarder.core.domain.ForwardingRepository
import com.samsung.android.otpforwarder.core.model.ForwardingStatus
import com.samsung.android.otpforwarder.core.sms.SmsEventBus
import com.samsung.android.otpforwarder.core.sms.SmsReceiver
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Instrumentation tests for [SmsReceiver] → [ForwardingRepository] + [SmsEventBus].
 *
 * These tests call [SmsReceiver.onReceive] directly with a crafted intent
 * (no real radio involved), verify that OTPs are recorded in the in-memory
 * repository, and confirm that [SmsEventBus] received the event.
 *
 * ## What's NOT tested here
 * - Actual SMS delivery (requires SEND_SMS permission + real SIM/emulator radio)
 * - WorkManager job enqueueing — tested implicitly since [ForwardingDispatcher]
 *   is started automatically in [HiltTestApplication] once Hilt injects singletons.
 *
 * ## Running
 * ```
 * ./gradlew :app:connectedAndroidTest
 * ```
 * or via Android Studio → right-click the test class → Run.
 *
 * ## Manual end-to-end testing on emulator
 * 1. Launch the app.
 * 2. Grant RECEIVE_SMS + SEND_SMS permissions (App Info → Permissions).
 * 3. Configure a destination phone number in Settings.
 * 4. In a terminal: `adb emu sms send +15555215554 "Your OTP is 123456"`
 * 5. Watch logcat for `SmsReceiver` and `ForwardingWorker` tags.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SmsReceiverTest {

    // Injects all @Inject fields from the Hilt component.
    // order=0 ensures the rule runs before any @Before methods.
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var forwardingRepository: ForwardingRepository
    @Inject lateinit var smsEventBus: SmsEventBus

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    // ── Tests ──────────────────────────────────────────────────────────────────

    /**
     * Verifies the happy path: an SMS with an OTP code reaches [ForwardingRepository]
     * with the correct code and PENDING status.
     */
    @Test
    fun receiveSms_withOtpMessage_createsForwardingRecord() {
        // Arrange
        val sender = "+15555215554"
        val body   = "Your OTP is 123456. Valid for 5 minutes."
        val intent = SmsTestPduUtils.buildSmsReceivedIntent(sender, body)

        val receiver = SmsReceiver()

        // Act
        receiver.onReceive(context, intent)

        // Assert — repository should have exactly one record
        val records = forwardingRepository.records.value
        assertEquals("Expected one forwarding record", 1, records.size)

        val record = records.first()
        assertEquals("Extracted OTP code mismatch",   "123456",           record.otpCode)
        assertEquals("Sender mismatch",                sender,             record.sender)
        assertEquals("Status should start as PENDING", ForwardingStatus.PENDING, record.status)
        assertTrue("Full body should be stored", record.fullBody.contains("123456"))
    }

    /**
     * Verifies that a plain SMS (no keyword, no code) is NOT stored as a
     * forwarding record (it's still monitored/notified, just not forwarded).
     */
    @Test
    fun receiveSms_withNonOtpMessage_doesNotCreateForwardingRecord() {
        // Arrange
        val intent = SmsTestPduUtils.buildSmsReceivedIntent(
            sender = "+10000000000",
            body   = "Hey, are you free for lunch tomorrow?",
        )
        val receiver = SmsReceiver()

        // Act
        receiver.onReceive(context, intent)

        // Assert — repository should be empty
        val records = forwardingRepository.records.value
        assertTrue("Non-OTP message should not create a record", records.isEmpty())
    }

    /**
     * Verifies that [SmsEventBus] emits an [OtpEvent] when an OTP is detected,
     * so that [ForwardingDispatcher] (and any other subscribers) are notified.
     */
    @Test
    fun receiveSms_withOtpMessage_emitsEventOnBus() = runTest {
        // Arrange
        val sender = "+15555215554"
        val intent = SmsTestPduUtils.buildSmsReceivedIntent(
            sender = sender,
            body   = "Your authentication code is 654321",
        )
        val receiver = SmsReceiver()

        // Collect the first event from the bus with a timeout.
        var capturedCode: String? = null
        val collectJob = launch {
            withTimeout(5.seconds) {
                smsEventBus.events.first { event ->
                    capturedCode = event.otpCode
                    true
                }
            }
        }

        // Act
        receiver.onReceive(context, intent)

        // Wait for the collect job to complete or time out
        collectJob.join()

        // Assert
        assertEquals("Event bus OTP code mismatch", "654321", capturedCode)
    }

    /**
     * Verifies that multiple distinct OTP messages each create their own record.
     * Ensures the receiver handles burst SMS correctly.
     */
    @Test
    fun receiveSms_multiplOtpMessages_createsMultipleRecords() {
        val receiver = SmsReceiver()

        receiver.onReceive(
            context,
            SmsTestPduUtils.buildSmsReceivedIntent("+15551111111", "OTP: 111111"),
        )
        receiver.onReceive(
            context,
            SmsTestPduUtils.buildSmsReceivedIntent("+15552222222", "OTP: 222222"),
        )

        val records = forwardingRepository.records.value
        assertEquals("Expected two records for two OTP SMS", 2, records.size)

        val codes = records.map { it.otpCode }.toSet()
        assertTrue("111111 should be recorded", "111111" in codes)
        assertTrue("222222 should be recorded", "222222" in codes)
    }

    /**
     * Verifies that different OTP keyword formats are all detected correctly.
     * Tests the breadth of [OtpDetectorImpl.KEYWORD_REGEX].
     */
    @Test
    fun receiveSms_variousOtpKeywords_allDetected() {
        val receiver = SmsReceiver()
        val testCases = listOf(
            "Your OTP is 100001"                    to "100001",
            "Verification code: 200002"             to "200002",
            "Use passcode 300003 to sign in"        to "300003",
            "Enter pin 4444 in the app"             to "4444",
            "Token 55555 expires in 10 minutes"     to "55555",
            "Your one-time code: 666666"            to "666666",
        )

        testCases.forEachIndexed { idx, (body, expectedCode) ->
            receiver.onReceive(
                context,
                SmsTestPduUtils.buildSmsReceivedIntent("+1555000${idx + 1}", body),
            )
        }

        val records = forwardingRepository.records.value
        assertEquals("Expected ${testCases.size} records", testCases.size, records.size)

        val detectedCodes = records.map { it.otpCode }.toSet()
        testCases.forEach { (_, expectedCode) ->
            assertNotNull("Code $expectedCode was not detected", detectedCodes.find { it == expectedCode })
        }
    }
}
