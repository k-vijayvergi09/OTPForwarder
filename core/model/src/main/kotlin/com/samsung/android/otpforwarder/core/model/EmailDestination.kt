package com.samsung.android.otpforwarder.core.model

/**
 * An email-address destination that the user has whitelisted for OTP forwarding.
 *
 * Mirrors [SmsDestination] but for email channels. Email forwarding itself is
 * scheduled for milestone M3 — until then [EmailDestination]s are persisted
 * and editable from the UI but [ForwardingWorker] skips them.
 *
 * @param id           Stable UUID.
 * @param label        Optional human-friendly tag, e.g. "Personal", "Work".
 * @param emailAddress RFC 5322 address (loose validation only — the UI rejects
 *                     obviously malformed input, the backend does the real check).
 * @param isEnabled    Whether this destination should receive forwarded OTPs.
 */
data class EmailDestination(
    val id: String,
    val label: String,
    val emailAddress: String,
    val isEnabled: Boolean = true,
)
