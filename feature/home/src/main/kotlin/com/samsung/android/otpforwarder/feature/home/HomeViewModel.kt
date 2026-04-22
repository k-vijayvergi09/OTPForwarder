package com.samsung.android.otpforwarder.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.otpforwarder.core.domain.ForwardingRepository
import com.samsung.android.otpforwarder.core.model.DestinationType
import com.samsung.android.otpforwarder.core.model.ForwardingRecord
import com.samsung.android.otpforwarder.core.model.ForwardingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ForwardingRepository,
) : ViewModel(), ContainerHost<HomeState, HomeSideEffect> {

    override val container = container<HomeState, HomeSideEffect>(HomeState(isLoading = true))

    init {
        observeData()
    }

    // ── Observation ───────────────────────────────────────────────────────────

    private fun observeData() {
        combine(
            repository.todayRecords(),
            repository.todayStats(),
        ) { records, stats ->
            val items = records
                .take(10) // Home shows at most 10 recent entries
                .map { it.toRowUiItem(fullMask = true) }
            Pair(items, stats)
        }.onEach { (items, stats) ->
            intent {
                reduce {
                    state.copy(
                        isLoading      = false,
                        recentItems    = items,
                        todayCount     = stats.total,
                        activeRulesCount = 0, // wired in M2 when RulesRepository exists
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    // ── Intents ───────────────────────────────────────────────────────────────

    fun onIntent(intent: HomeIntent) = when (intent) {
        HomeIntent.ToggleForwarding -> intent {
            reduce { state.copy(isForwardingEnabled = !state.isForwardingEnabled) }
        }
        HomeIntent.NavigateToLogs -> intent { postSideEffect(HomeSideEffect.GoToLogs) }
        HomeIntent.NavigateToSettings -> intent { postSideEffect(HomeSideEffect.GoToSettings) }
    }
}

// ── Mapper ────────────────────────────────────────────────────────────────────

private fun ForwardingRecord.toRowUiItem(fullMask: Boolean): OtpRowUiItem = OtpRowUiItem(
    id         = id,
    sender     = sender,
    maskedOtp  = if (fullMask) otpCode.fullyMasked() else otpCode.partialMask(),
    subtitle   = buildSubtitle(),
    status     = status,
    timeLabel  = receivedAt.toTimeLabel(),
)

private fun String.fullyMasked(): String {
    val groups = chunked(3).map { "•".repeat(it.length) }
    return groups.joinToString(" ")
}

private fun String.partialMask(): String {
    if (length <= 3) return this
    val visible = takeLast(3)
    val hidden  = "•".repeat(length - 3)
    return "$hidden $visible"
}

private fun ForwardingRecord.buildSubtitle(): String = when (status) {
    ForwardingStatus.FORWARDED -> when {
        destinations.containsAll(listOf(DestinationType.SMS, DestinationType.EMAIL)) ->
            "Forwarded · SMS + Email"
        destinations.contains(DestinationType.SMS) -> "Forwarded · SMS"
        destinations.contains(DestinationType.EMAIL) -> "Forwarded · Email"
        else -> "Forwarded"
    }
    ForwardingStatus.PENDING       -> "Queued"
    ForwardingStatus.RETRY_QUEUED  -> errorMessage?.let { "Retry queued" } ?: "Retry queued"
    ForwardingStatus.FAILED        -> errorMessage ?: "Failed"
}

private fun kotlinx.datetime.Instant.toTimeLabel(): String {
    val diff = Clock.System.now() - this
    val todayDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val thisDate  = toLocalDateTime(TimeZone.currentSystemDefault()).date
    return when {
        diff < 1.minutes  -> "just now"
        diff < 1.hours    -> "${diff.inWholeMinutes}m ago"
        thisDate == todayDate -> "${diff.inWholeHours}h ago"
        else -> {
            val ldt = toLocalDateTime(TimeZone.currentSystemDefault())
            "yday ${ldt.hour}:${ldt.minute.toString().padStart(2, '0')}"
        }
    }
}
