package com.samsung.android.otpforwarder.core.network

import com.samsung.android.otpforwarder.core.domain.EmailCredentialRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton
import javax.mail.AuthenticationFailedException
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Sends emails via Gmail's SMTP server using STARTTLS on port 587.
 *
 * Two public entry points:
 *  - [send]            — used by [ForwardingWorker] for live OTP delivery.
 *  - [testConnection]  — used by the Gmail setup screen to validate credentials
 *                        before saving, without requiring credentials to be
 *                        persisted first.
 *
 * All SMTP I/O runs on [Dispatchers.IO].
 */
@Singleton
class EmailSender @Inject constructor(
    private val credentialRepository: EmailCredentialRepository,
) {

    /**
     * Send a single email to [to] using the stored Gmail credentials.
     * Returns [EmailSendResult.Failure] if no credentials are configured.
     */
    suspend fun send(
        to: String,
        subject: String,
        body: String,
    ): EmailSendResult {
        val credentials = credentialRepository.get()
            ?: return EmailSendResult.Failure("Gmail account not configured")
        return sendWithCredentials(
            from     = credentials.address,
            password = credentials.appPassword,
            to       = to,
            subject  = subject,
            body     = body,
        )
    }

    /**
     * Validates the given credentials by attempting to open an authenticated
     * SMTP session and sending a test message to the same address.
     * Used by the setup screen before committing credentials to storage.
     */
    suspend fun testConnection(
        address: String,
        appPassword: String,
    ): EmailSendResult = sendWithCredentials(
        from     = address,
        password = appPassword,
        to       = address,
        subject  = "OTP Forwarder — connection test",
        body     = "Your Gmail account is set up correctly. OTPs will be forwarded to your configured destinations.",
    )

    // ── Core SMTP logic ───────────────────────────────────────────────────────

    private suspend fun sendWithCredentials(
        from: String,
        password: String,
        to: String,
        subject: String,
        body: String,
    ): EmailSendResult = withContext(Dispatchers.IO) {
        try {
            val props = smtpProperties()
            val session = Session.getInstance(props, object : javax.mail.Authenticator() {
                override fun getPasswordAuthentication() =
                    javax.mail.PasswordAuthentication(from, password)
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(from))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                this.subject = subject
                setText(body, "utf-8")
            }

            // Transport.send is blocking — fine on Dispatchers.IO
            javax.mail.Transport.send(message)

            Timber.i("EmailSender: sent to %s (subject=%s)", to, subject)
            EmailSendResult.Success
        } catch (e: AuthenticationFailedException) {
            Timber.w(e, "EmailSender: authentication failed for %s", from)
            EmailSendResult.Failure(
                reason = "Authentication failed — check your Gmail address and App Password",
                cause  = e,
            )
        } catch (e: MessagingException) {
            Timber.e(e, "EmailSender: SMTP error sending to %s", to)
            EmailSendResult.Failure(
                reason = "SMTP error: ${e.message ?: "unknown error"}",
                cause  = e,
            )
        } catch (e: Exception) {
            Timber.e(e, "EmailSender: unexpected error sending to %s", to)
            EmailSendResult.Failure(
                reason = "Unexpected error: ${e.message ?: "unknown"}",
                cause  = e,
            )
        }
    }

    private fun smtpProperties(): Properties = Properties().apply {
        put("mail.smtp.auth",                "true")
        put("mail.smtp.starttls.enable",     "true")
        put("mail.smtp.starttls.required",   "true")
        put("mail.smtp.host",                SMTP_HOST)
        put("mail.smtp.port",                SMTP_PORT)
        // Restrict to modern TLS versions only
        put("mail.smtp.ssl.protocols",       "TLSv1.2 TLSv1.3")
        // Connection + read timeout (ms) — prevents the worker hanging indefinitely
        put("mail.smtp.connectiontimeout",   "15000")
        put("mail.smtp.timeout",             "15000")
        put("mail.smtp.writetimeout",        "15000")
    }

    private companion object {
        const val SMTP_HOST = "smtp.gmail.com"
        const val SMTP_PORT = "587"
    }
}
