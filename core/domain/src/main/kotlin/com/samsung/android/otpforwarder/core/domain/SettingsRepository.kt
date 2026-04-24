package com.samsung.android.otpforwarder.core.domain

import com.samsung.android.otpforwarder.core.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun updateSettings(transform: suspend (AppSettings) -> AppSettings)
}
