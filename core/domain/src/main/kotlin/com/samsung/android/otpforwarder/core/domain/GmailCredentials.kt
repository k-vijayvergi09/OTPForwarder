package com.samsung.android.otpforwarder.core.domain

/**
 * The Gmail credentials the user has configured for outbound SMTP forwarding.
 *
 * @param address     The full Gmail address, e.g. "you@gmail.com".
 * @param appPassword The 16-character App Password generated in the user's
 *                    Google Account (Manage Account → Security → App Passwords).
 *                    This is NOT the user's real Google password.
 */
data class GmailCredentials(
    val address: String,
    val appPassword: String,
)
