package com.samsung.android.otpforwarder.core.model

/** Summary counts for today's forwarding activity, shown in the Home status card and Logs summary bar. */
data class TodayStats(
    val forwarded: Int = 0,
    val failed: Int = 0,
    val pending: Int = 0,
) {
    val total: Int get() = forwarded + failed + pending
}
