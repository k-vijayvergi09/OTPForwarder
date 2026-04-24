package com.samsung.android.otpforwarder

import androidx.hilt.work.HiltWorkerFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.samsung.android.otpforwarder.core.domain.ForwardingRepository
import com.samsung.android.otpforwarder.core.domain.SettingsRepository
import com.samsung.android.otpforwarder.core.model.DestinationType
import com.samsung.android.otpforwarder.core.model.ForwardingRecord
import com.samsung.android.otpforwarder.core.model.ForwardingStatus
import com.samsung.android.otpforwarder.core.sms.ForwardingWorker
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import javax.inject.Inject

/**
 * Instrumentation tests for [ForwardingWorker].
 *
 * Uses [TestListenableWorkerBuilder] to run the worker synchronously in the test
 * process without going through the real WorkManager queue.  This lets us test
 * the worker's business logic — settings checks, record lookups, status updates —
 * without needing SEND_SMS permission or an actual SIM card.
 *
 * ## Test coverage
 * | Scenario                            | Expected result            |
 * |-------------------------------------|----------------------------|
 * | Missing event ID input data         | Result.failure()           |
 * | Forwarding globally disabled        | Result.success() (no-op)   |
 * | SMS destination not enabled         | Result.success() (no-op)   |
 * | SMS destination enabled, no number  | Result.failure()           |
 * | SEND_SMS permission denied          | Result.failure()           |
 *
 * The "happy path" (SEND_SMS granted, SMS actually sent) requires a real SIM and
 * cannot be tested in an emulator without granting the permission at the ADB level:
 *   `adb shell pm grant com.samsung.android.otpforwarder android.permission.SEND_SMS`
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ForwardingWorkerTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    // HiltWorkerFactory is @Singleton and directly injectable — it wires the
    // @HiltWorker / @AssistedInject constructor when TestListenableWorkerBuilder
    // calls WorkerFactory.createWorker().
    @Inject lateinit var workerFactory:        HiltWorkerFactory
    @Inject lateinit var forwardingRepository: ForwardingRepository
    @Inject lateinit var settingsRepository:   SettingsRepository

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private fun buildWorker(eventId: String? = null): ForwardingWorker =
        TestListenableWorkerBuilder<ForwardingWorker>(context)
            .setWorkerFactory(workerFactory)
            .apply {
                if (eventId != null) {
                    setInputData(workDataOf(ForwardingWorker.KEY_EVENT_ID to eventId))
                }
            }
            .build() as ForwardingWorker

    private suspend fun insertPendingRecord(eventId: String, phoneNumber: String = "+15555215554"): ForwardingRecord {
        val now = Clock.System.now()
        val record = ForwardingRecord(
            id           = eventId,
            otpCode      = "123456",
            sender       = phoneNumber,
            fullBody     = "Your OTP is 123456",
            status       = ForwardingStatus.PENDING,
            errorMessage = null,
            receivedAt   = now,
            updatedAt    = now,
        )
        forwardingRepository.addRecord(record)
        return record
    }

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    fun doWork_missingEventId_returnsFailure() = runBlocking {
        val result = buildWorker(eventId = null).doWork()
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun doWork_forwardingGloballyDisabled_returnsSuccessWithoutForwarding() = runBlocking {
        // Disable forwarding globally
        settingsRepository.updateSettings { it.copy(isForwardingEnabled = false) }

        val eventId = UUID.randomUUID().toString()
        insertPendingRecord(eventId)

        val result = buildWorker(eventId).doWork()

        // Worker should bail early with success (not a retryable error)
        assertEquals(ListenableWorker.Result.success(), result)

        // Status should be updated to FAILED with an explanatory message
        val record = forwardingRepository.records.value.find { it.id == eventId }
        assertEquals(ForwardingStatus.FAILED, record?.status)
    }

    @Test
    fun doWork_smsDestinationNotEnabled_returnsSuccess() = runBlocking {
        // Only EMAIL destination is enabled, not SMS
        settingsRepository.updateSettings {
            it.copy(
                isForwardingEnabled  = true,
                defaultDestinations  = listOf(DestinationType.EMAIL),
            )
        }

        val eventId = UUID.randomUUID().toString()
        insertPendingRecord(eventId)

        val result = buildWorker(eventId).doWork()

        // Skipping SMS forwarding is not an error — email forwarding may run elsewhere
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun doWork_smsEnabledButNoNumberConfigured_returnsFailure() = runBlocking {
        settingsRepository.updateSettings {
            it.copy(
                isForwardingEnabled  = true,
                defaultDestinations  = listOf(DestinationType.SMS),
                defaultPhoneNumber   = "",  // no number!
            )
        }

        val eventId = UUID.randomUUID().toString()
        insertPendingRecord(eventId)

        val result = buildWorker(eventId).doWork()

        assertEquals(ListenableWorker.Result.failure(), result)

        val record = forwardingRepository.records.value.find { it.id == eventId }
        assertEquals(ForwardingStatus.FAILED, record?.status)
    }

    @Test
    fun doWork_recordNotFound_returnsFailure() = runBlocking {
        settingsRepository.updateSettings {
            it.copy(
                isForwardingEnabled = true,
                defaultDestinations = listOf(DestinationType.SMS),
                defaultPhoneNumber  = "+15555215554",
            )
        }

        // Intentionally do NOT insert a record — worker should fail gracefully
        val result = buildWorker("nonexistent-event-id").doWork()
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun doWork_smsEnabledWithNumber_sendSmsPermissionDenied_returnsFailure() = runBlocking {
        // In the test environment SEND_SMS is not granted.
        // SmsSender.sendSms() returns false → worker returns failure.
        settingsRepository.updateSettings {
            it.copy(
                isForwardingEnabled = true,
                defaultDestinations = listOf(DestinationType.SMS),
                defaultPhoneNumber  = "+15555215554",
            )
        }

        val eventId = UUID.randomUUID().toString()
        insertPendingRecord(eventId)

        val result = buildWorker(eventId).doWork()

        // SEND_SMS denied → SmsSender returns false → worker fails
        assertEquals(ListenableWorker.Result.failure(), result)

        val record = forwardingRepository.records.value.find { it.id == eventId }
        assertEquals(ForwardingStatus.FAILED, record?.status)
    }
}
