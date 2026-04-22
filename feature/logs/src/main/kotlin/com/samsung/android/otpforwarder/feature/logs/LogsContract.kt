package com.samsung.android.otpforwarder.feature.logs

import com.samsung.android.otpforwarder.core.model.ForwardingStatus

// ── UI models ─────────────────────────────────────────────────────────────────

data class LogRowUiItem(
    val id: String,
    val sender: String,
    /** Last 3 digits visible, e.g. "••• 842". Fully masked when PENDING/FAILED. */
    val maskedOtp: String,
    /** Destination summary, e.g. "SMS → +91 98••• • · Email delivered" */
    val deliveryLine: String,
    val status: ForwardingStatus,
    val timeLabel: String,
)

data class LogGroup(
    /** Section label e.g. "TODAY" or "YESTERDAY" or "MON, 21 APR" */
    val label: String,
    val items: List<LogRowUiItem>,
)

// ── MVI contract ──────────────────────────────────────────────────────────────

data class LogsState(
    val todayForwarded: Int = 0,
    val todayFailed: Int = 0,
    val todayPending: Int = 0,
    val groups: List<LogGroup> = emptyList(),
    val isLoading: Boolean = false,
)

sealed interface LogsIntent {
    data object NavigateBack : LogsIntent
    data class OpenDetail(val id: String) : LogsIntent
}

sealed interface LogsSideEffect {
    data object GoBack : LogsSideEffect
    data class ShowDetail(val id: String) : LogsSideEffect
}
