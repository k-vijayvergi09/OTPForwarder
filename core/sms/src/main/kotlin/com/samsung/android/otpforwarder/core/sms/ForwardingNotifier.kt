package com.samsung.android.otpforwarder.core.sms

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts system notifications for OTP forwarding outcomes:
 *  - [notifyForwarded]    — OTP successfully sent to the destination.
 *  - [notifySendFailed]   — A send attempt failed; updates the same notification
 *                           slot across retries (using [notificationId]) so the
 *                           user sees "Attempt 2 of 3 failed" rather than a flood
 *                           of separate notifications.
 *  - [cancelFailureNotification] — Dismisses the failure notification when a
 *                           later retry eventually succeeds.
 *
 * This is separate from [IncomingSmsNotifier] (which fires on every incoming SMS)
 * so that users can independently control "received" vs "forwarded" notification
 * visibility in system settings.
 *
 * Channel: [CHANNEL_ID] ("Forwarding Events", default importance).
 */
@Singleton
class ForwardingNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Post a success notification.
     *
     * Also call [cancelFailureNotification] with the same [notificationId] before
     * this if previous attempts had already posted a failure notification, so the
     * user is not left with a stale "failed" notification alongside the success one.
     *
     * @param sender         Originating SMS address shown in the notification body.
     * @param destination    The phone number the OTP was forwarded to.
     */
    fun notifyForwarded(sender: String, destination: String) {
        if (!isPostNotificationsGranted()) return
        ensureChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle("OTP forwarded")
            .setContentText("From $sender → $destination")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(getLogsPendingIntent())
            .setAutoCancel(true)
            .build()

        // Use a unique notification ID per forward so rapid OTPs don't collapse
        // into a single notification and suppress earlier ones silently.
        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID_BASE + (System.currentTimeMillis() % 1000).toInt(), notification)

        Timber.i("ForwardingNotifier: notified forwarded OTP from=%s to=%s", sender, destination)
    }

    /**
     * Post (or update) a failure/retry notification for a specific OTP event.
     *
     * Passing the same [notificationId] on every attempt for the same event means
     * Android replaces the previous notification in-place, so the user sees a
     * single updating notification rather than one per attempt.
     *
     * @param sender           Originating SMS address.
     * @param attemptNumber    1-indexed attempt count (1 = first try, 2 = first retry, …).
     * @param maxAttempts      Total attempts that will be made before giving up.
     * @param willRetry        `true` if WorkManager will schedule another attempt;
     *                         `false` if this is the terminal failure.
     * @param notificationId   Stable ID derived from the OTP event (e.g. eventId.hashCode())
     *                         so retries overwrite the same notification slot.
     */
    fun notifySendFailed(
        sender: String,
        eventId: String,
        attemptNumber: Int,
        maxAttempts: Int,
        willRetry: Boolean,
        notificationId: Int,
    ) {
        if (!isPostNotificationsGranted()) return
        ensureChannel()

        val title: String
        val body: String
        val icon: Int

        if (willRetry) {
            title = "OTP forwarding retrying"
            body  = "Attempt $attemptNumber of $maxAttempts failed from $sender — retrying"
            icon  = android.R.drawable.stat_sys_warning
        } else {
            title = "OTP forwarding failed"
            body  = "All $maxAttempts attempts failed for OTP from $sender"
            icon  = android.R.drawable.stat_notify_error
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(getLogsPendingIntent())
            .setAutoCancel(true)
            
        if (!willRetry) {
            val retryIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_RETRY_FORWARDING
                putExtra(NotificationActionReceiver.EXTRA_EVENT_ID, eventId)
            }
            val retryPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId, // using notificationId as request code
                retryIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_popup_sync,
                "Retry Now",
                retryPendingIntent
            )
        }

        val notification = builder.build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)

        Timber.w(
            "ForwardingNotifier: send failed attempt=%d/%d willRetry=%b sender=%s",
            attemptNumber, maxAttempts, willRetry, sender,
        )
    }

    /**
     * Dismiss the failure notification for [notificationId].
     * Call this on a successful retry so the user isn't left with a stale
     * "failed" notification alongside the new success one.
     */
    fun cancelFailureNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
        Timber.d("ForwardingNotifier: cancelled failure notification id=%d", notificationId)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun getLogsPendingIntent(): PendingIntent {
        val intent = Intent().apply {
            setClassName(context, "com.samsung.android.otpforwarder.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_NAVIGATE_TO", "logs")
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun isPostNotificationsGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Timber.d("ForwardingNotifier: POST_NOTIFICATIONS not granted — skipping")
                return false
            }
        }
        return true
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Forwarding Events",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Confirms each OTP was successfully forwarded to the destination number"
            },
        )
    }

    private companion object {
        const val CHANNEL_ID           = "forwarding_events"
        const val NOTIFICATION_ID_BASE = 3000
    }
}
