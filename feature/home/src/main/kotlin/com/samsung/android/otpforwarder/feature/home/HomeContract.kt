package com.samsung.android.otpforwarder.feature.home

import com.samsung.android.otpforwarder.core.model.ForwardingStatus

// ── UI model ──────────────────────────────────────────────────────────────────

data class OtpRowUiItem(
    val id: String,
    val sender: String,
    /** Fully masked, e.g. "••• •••" — shown on Home for privacy. */
    val maskedOtp: String,
    /** e.g. "Forwarded · SMS + Email", "Retry in 8s", "Network error" */
    val subtitle: String,
    val status: ForwardingStatus,
    /** e.g. "2m ago", "18m ago" */
    val timeLabel: String,
)

// ── MVI contract ──────────────────────────────────────────────────────────────

data class HomeState(
    val isForwardingEnabled: Boolean = true,
    val activeRulesCount: Int = 0,
    val todayCount: Int = 0,
    val recentItems: List<OtpRowUiItem> = emptyList(),
    val isLoading: Boolean = false,
)

sealed interface HomeIntent {
    data object ToggleForwarding : HomeIntent
    data object NavigateToLogs : HomeIntent
    data object NavigateToSettings : HomeIntent
}

sealed interface HomeSideEffect {
    data object GoToLogs : HomeSideEffect
    data object GoToSettings : HomeSideEffect
}
