package com.samsung.android.otpforwarder.core.model

/**
 * A phone-number destination that the user has whitelisted for OTP forwarding.
 *
 * In the simplified v1.1 model (per 2026-05-12 design decision), there is no
 * per-sender routing — every detected OTP fans out to every [SmsDestination]
 * with [isEnabled] = true.
 *
 * @param id          Stable UUID, generated on first creation.
 * @param label       Optional human-friendly tag, e.g. "Personal", "Work".
 *                    May be blank — UIs should fall back to [phoneNumber] for display.
 * @param phoneNumber E.164-style number (validated via [PhoneNumberValidator]).
 * @param isEnabled   When false the destination is skipped during forwarding but
 *                    kept in the list so the user can toggle it back on later.
 */
data class SmsDestination(
    val id: String,
    val label: String,
    val phoneNumber: String,
    val isEnabled: Boolean = true,
)
