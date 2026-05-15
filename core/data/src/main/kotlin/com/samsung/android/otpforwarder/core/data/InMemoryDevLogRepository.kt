package com.samsung.android.otpforwarder.core.data

import com.samsung.android.otpforwarder.core.domain.DevLogRepository
import com.samsung.android.otpforwarder.core.model.DevLog
import com.samsung.android.otpforwarder.core.model.DevLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory implementation of [DevLogRepository].
 *
 * Uses [MutableStateFlow] for reactive delivery of log updates to the UI.
 * Thread-safety is provided by [MutableStateFlow.update], which is an
 * atomic compare-and-set loop backed by [kotlin.concurrent.AtomicReference].
 *
 * Logs are intentionally ephemeral: they live only for the process lifetime
 * and are never written to disk. This keeps the implementation simple and
 * ensures no sensitive OTP content is persisted beyond what Room already stores.
 */
@Singleton
class InMemoryDevLogRepository @Inject constructor() : DevLogRepository {

    private val _logs = MutableStateFlow<Map<String, DevLog>>(emptyMap())
    override val logs: StateFlow<Map<String, DevLog>> = _logs.asStateFlow()

    override fun log(eventId: String, entry: DevLogEntry) {
        _logs.update { current ->
            val existing = current[eventId] ?: DevLog(eventId = eventId)
            current + (eventId to existing.copy(entries = existing.entries + entry))
        }
    }

    override fun getLog(eventId: String): DevLog? = _logs.value[eventId]

    override fun clearLog(eventId: String) {
        _logs.update { it - eventId }
    }
}
