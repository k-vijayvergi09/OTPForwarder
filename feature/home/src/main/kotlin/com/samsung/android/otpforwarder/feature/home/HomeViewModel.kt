package com.samsung.android.otpforwarder.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.otpforwarder.core.domain.EmailDestinationRepository
import com.samsung.android.otpforwarder.core.domain.ForwardingRepository
import com.samsung.android.otpforwarder.core.domain.SmsDestinationRepository
import com.samsung.android.otpforwarder.core.model.DestinationType
import com.samsung.android.otpforwarder.core.model.ForwardingRecord
import com.samsung.android.otpforwarder.core.model.ForwardingStatus
import com.samsung.android.otpforwarder.core.sms.ForwardingDispatcher
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
    private val dispatcher: ForwardingDispatcher,
    private val smsDestinationRepository: SmsDestinationRepository,
    private val emailDestinationRepository: EmailDestinationRepository,
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
            smsDestinationRepository.observeEnabled(),
            emailDestinationRepository.observeEnabled(),
        ) { records, stats, smsEnabled, emailEnabled ->
            val items = records
                .take(10) // Home shows at most 10 recent entries
                .map { it.toRowUiItem() }
            Triple(items, stats, smsEnabled.size + emailEnabled.size)
        }.onEach { (items, stats, destinationsCount) ->
            intent {
                reduce {
                    state.copy(
                        isLoading               = false,
                        recentItems             = items,
                        todayCount              = stats.total,
                        activeDestinationsCount = destinationsCount,
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
        is HomeIntent.RetryForwarding -> intent {
            repository.updateStatus(intent.id, ForwardingStatus.PENDING, null)
            dispatcher.forceRetry(intent.id)
        }
    }
}

// ── Mapper ────────────────────────────────────────────────────────────────────

private fun ForwardingRecord.toRowUiItem(): OtpRowUiItem = OtpRowUiItem(
    id        = id,
    sender    = sender,
    otp       = otpCode,
    subtitle  = buildSubtitle(),
    status    = status,
    timeLabel = receivedAt.toTimeLabel(),
)

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
