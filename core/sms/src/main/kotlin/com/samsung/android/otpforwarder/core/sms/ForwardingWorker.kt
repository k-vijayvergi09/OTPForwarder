package com.samsung.android.otpforwarder.core.sms

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.samsung.android.otpforwarder.core.domain.ForwardingRepository
import com.samsung.android.otpforwarder.core.domain.SettingsRepository
import com.samsung.android.otpforwarder.core.model.DestinationType
import com.samsung.android.otpforwarder.core.model.ForwardingStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

@HiltWorker
class ForwardingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val forwardingRepository: ForwardingRepository,
    private val settingsRepository: SettingsRepository,
    private val smsSender: SmsSender,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val eventId = inputData.getString(KEY_EVENT_ID)
        if (eventId == null) {
            Timber.e("ForwardingWorker: No event ID provided")
            return Result.failure()
        }

        val settings = settingsRepository.settings.first()
        if (!settings.isForwardingEnabled) {
            Timber.i("ForwardingWorker: Forwarding is disabled globally")
            forwardingRepository.updateStatus(eventId, ForwardingStatus.FAILED, "Forwarding disabled")
            return Result.success()
        }

        val records = forwardingRepository.records.first()
        val record = records.find { it.id == eventId }
        if (record == null) {
            Timber.e("ForwardingWorker: Record not found for id \$eventId")
            return Result.failure()
        }

        if (DestinationType.SMS !in settings.defaultDestinations) {
            Timber.i("ForwardingWorker: SMS destination not enabled")
            return Result.success()
        }

        val destinationNumber = settings.defaultPhoneNumber
        if (destinationNumber.isBlank()) {
            Timber.w("ForwardingWorker: SMS destination enabled but no number configured")
            forwardingRepository.updateStatus(eventId, ForwardingStatus.FAILED, "No destination number")
            return Result.failure()
        }

        val messageBody = "FWD from \${record.sender}: \${record.fullBody}"
        val success = smsSender.sendSms(destinationNumber, messageBody)

        return if (success) {
            forwardingRepository.updateStatus(eventId, ForwardingStatus.FORWARDED, null)
            Result.success()
        } else {
            forwardingRepository.updateStatus(eventId, ForwardingStatus.FAILED, "SMS dispatch failed")
            Result.failure()
        }
    }

    companion object {
        const val KEY_EVENT_ID = "event_id"
    }
}
