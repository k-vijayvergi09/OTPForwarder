package com.samsung.android.otpforwarder.feature.logs

import com.samsung.android.otpforwarder.core.model.ForwardingStatus

// ── UI models ─────────────────────────────────────────────────────────────────

data class LogRowUiItem(
    val id: String,
    val sender: String,
    /** The extracted OTP code as-is, e.g. "842913", "4127". No masking applied. */
    val otp: String,
    val fullBody: String,
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
    /** All records grouped by date, unfiltered. */
    val groups: List<LogGroup> = emptyList(),
    val isLoading: Boolean = false,
    val selectedLog: LogRowUiItem? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
) {
    /** Groups filtered by the current search query (case-insensitive). */
    val filteredGroups: List<LogGroup> get() {
        if (searchQuery.isBlank()) return groups
        val q = searchQuery.trim().lowercase()
        return groups
            .mapNotNull { group ->
                val matched = group.items.filter { item ->
                    item.sender.lowercase().contains(q) ||
                    item.otp.contains(q) ||
                    item.fullBody.lowercase().contains(q)
                }
                if (matched.isEmpty()) null else group.copy(items = matched)
            }
    }
}

sealed interface LogsIntent {
    data object NavigateBack : LogsIntent
    data class OpenDetail(val id: String) : LogsIntent
    data object CloseDetail : LogsIntent
    data class RetryForwarding(val id: String) : LogsIntent
    data class SetSearchQuery(val query: String) : LogsIntent
    data object ToggleSearch : LogsIntent
}

sealed interface LogsSideEffect {
    data object GoBack : LogsSideEffect
}
