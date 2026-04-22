package com.samsung.android.otpforwarder.core.model

/**
 * A user-configured rule that determines *which* SMS senders trigger forwarding
 * and *where* the OTP is forwarded.
 *
 * @param id             Stable UUID.
 * @param name           User-visible label, e.g. "HDFC Bank".
 * @param senderPattern  Substring / regex fragment matched against the SMS sender address.
 *                       Empty string means "match all senders" (the default catch-all rule).
 * @param destinations   One or more channels to forward to.
 * @param isEnabled      Whether this rule is currently active.
 * @param priority       Rules are evaluated in ascending priority order; lower = higher priority.
 */
data class ForwardingRule(
    val id: String,
    val name: String,
    val senderPattern: String,
    val destinations: List<DestinationType>,
    val isEnabled: Boolean = true,
    val priority: Int = 0,
) {
    /** True if this rule matches all senders (catch-all). */
    val isCatchAll: Boolean get() = senderPattern.isBlank()
}
