package com.samsung.android.otpforwarder.core.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsSender @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun sendSms(destinationNumber: String, messageBody: String): Boolean {
        if (destinationNumber.isBlank()) {
            Timber.w("SmsSender: Destination number is blank.")
            return false
        }

        val permissionState = ContextCompat.checkSelfPermission(
            context, Manifest.permission.SEND_SMS
        )
        if (permissionState != PackageManager.PERMISSION_GRANTED) {
            Timber.w("SmsSender: SEND_SMS permission not granted.")
            return false
        }

        return try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            if (smsManager == null) {
                Timber.e("SmsSender: SmsManager is null")
                return false
            }

            val parts = smsManager.divideMessage(messageBody)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(destinationNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(destinationNumber, null, messageBody, null, null)
            }
            Timber.i("SmsSender: Successfully dispatched SMS to \$destinationNumber")
            true
        } catch (e: Exception) {
            Timber.e(e, "SmsSender: Failed to send SMS")
            false
        }
    }
}
