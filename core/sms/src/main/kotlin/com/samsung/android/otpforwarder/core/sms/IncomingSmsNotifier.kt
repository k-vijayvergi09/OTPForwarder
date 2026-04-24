package com.samsung.android.otpforwarder.core.sms

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomingSmsNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun notifyIncomingMessage(sender: String, preview: String) {
        Log.i(TAG, "notifyIncomingMessage sender=${sender.ifBlank { "Unknown sender" }}")
        ensureChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            Log.i(TAG, "POST_NOTIFICATIONS granted=$granted")
            if (!granted) {
                Log.i(TAG, "Skipping notification because permission is missing")
                return
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle("Incoming SMS detected")
            .setContentText(sender.ifBlank { "Unknown sender" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(preview))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        Log.i(TAG, "Notification posted channel=$CHANNEL_ID id=$NOTIFICATION_ID")
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.i(TAG, "Notification channel not required on this Android version")
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) {
            Log.i(TAG, "Notification channel already exists: $CHANNEL_ID")
            return
        }

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Incoming SMS",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alerts when OTP Forwarder observes a new SMS"
            },
        )
        Log.i(TAG, "Created notification channel: $CHANNEL_ID")
    }

    private companion object {
        const val TAG = "OtpForwarderNotify"
        const val CHANNEL_ID = "incoming_sms"
        const val NOTIFICATION_ID = 2001
    }
}
