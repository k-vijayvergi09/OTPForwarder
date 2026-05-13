package com.samsung.android.otpforwarder.feature.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.otpforwarder.core.domain.ForwardingRepository
import com.samsung.android.otpforwarder.core.model.DestinationType
import com.samsung.android.otpforwarder.core.model.ForwardingRecord
import com.samsung.android.otpforwarder.core.model.ForwardingStatus
import com.samsung.android.otpforwarder.core.sms.ForwardingDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val repository: ForwardingRepository,
    private val dispatcher: ForwardingDispatcher,
) : ViewModel(), ContainerHost<LogsState, LogsSideEffect> {

    override val container = container<LogsState, LogsSideEffect>(LogsState(isLoading = true))

    init { observeData() }

    private fun observeData() {
        combine(repository.records, repository.todayStats()) { records, stats ->
            Pair(records, stats)
        }.onEach { (records, stats) ->
            val groups = records.groupByDate()
            intent {
                reduce {
                    state.copy(
                        isLoading      = false,
                        todayForwarded = stats.forwarded,
                        todayFailed    = stats.failed,
                        todayPending   = stats.pending,
                        groups         = groups,
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onIntent(intent: LogsIntent) = when (intent) {
        LogsIntent.NavigateBack       -> intent { postSideEffect(LogsSideEffect.GoBack) }
        is LogsIntent.OpenDetail      -> intent {
            val log = state.groups.flatMap { it.items }.find { it.id == intent.id }
            reduce { state.copy(selectedLog = log) }
        }
        LogsIntent.CloseDetail        -> intent { reduce { state.copy(selectedLog = null) } }
        is LogsIntent.RetryForwarding -> intent {
            repository.updateStatus(intent.id, ForwardingStatus.PENDING, null)
            dispatcher.forceRetry(intent.id)
        }
        is LogsIntent.SetSearchQuery  -> intent {
            reduce { state.copy(searchQuery = intent.query) }
        }
        LogsIntent.ToggleSearch       -> intent {
            reduce { state.copy(
                isSearchActive = !state.isSearchActive,
                searchQuery    = "",  // clear query whenever the bar is toggled
            ) }
        }
    }
}

// ── Grouping + mapping ────────────────────────────────────────────────────────

private fun List<ForwardingRecord>.groupByDate(): List<LogGroup> {
    val tz       = TimeZone.currentSystemDefault()
    val today    = Clock.System.now().toLocalDateTime(tz).date
    val yesterday = today.minus(1, DateTimeUnit.DAY)

    return groupBy { it.receivedAt.toLocalDateTime(tz).date }
        .entries
        .sortedByDescending { it.key }
        .map { (date, records) ->
            val label = when (date) {
                today     -> "TODAY"
                yesterday -> "YESTERDAY"
                else      -> "${date.dayOfWeek.name.take(3)}, ${date.dayOfMonth} ${date.month.name.take(3)}"
            }
            LogGroup(label = label, items = records.map { it.toLogRowUiItem() })
        }
}

private fun ForwardingRecord.toLogRowUiItem(): LogRowUiItem = LogRowUiItem(
    id           = id,
    sender       = sender,
    otp          = otpCode,
    fullBody     = fullBody,
    deliveryLine = buildDeliveryLine(),
    status       = status,
    timeLabel    = receivedAt.toTimeLabel(),
)

private fun ForwardingRecord.buildDeliveryLine(): String = when (status) {
    ForwardingStatus.FORWARDED -> {
        val parts = mutableListOf<String>()
        if (destinations.contains(DestinationType.SMS))   parts += "SMS delivered"
        if (destinations.contains(DestinationType.EMAIL)) parts += "Email delivered"
        if (parts.isEmpty()) "Forwarded" else parts.joinToString(" · ")
    }
    ForwardingStatus.PENDING       -> "Queued"
    ForwardingStatus.RETRY_QUEUED  -> "Retrying"
    ForwardingStatus.FAILED        -> errorMessage ?: "Failed"
}

private fun kotlinx.datetime.Instant.toTimeLabel(): String {
    val diff  = Clock.System.now() - this
    val tz    = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(tz).date
    val date  = toLocalDateTime(tz).date
    return when {
        diff < 1.minutes  -> "just now"
        diff < 1.hours    -> "${diff.inWholeMinutes}m ago"
        date == today     -> "${diff.inWholeHours}h ago"
        else -> {
            val ldt = toLocalDateTime(tz)
            "yday ${ldt.hour}:${ldt.minute.toString().padStart(2, '0')}"
        }
    }
}
