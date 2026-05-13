package com.samsung.android.otpforwarder.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.samsung.android.otpforwarder.core.domain.SettingsRepository
import com.samsung.android.otpforwarder.core.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SettingsRepository] backed by Jetpack [DataStore] with [Preferences].
 *
 * All keys are stable string constants — changing them is a breaking migration.
 *
 * Note (2026-05-13): the destination-related fields (default phone, default email,
 * default destinations) were removed from [AppSettings] when destinations moved to
 * their own Room-backed tables. The legacy keys are intentionally NOT read here —
 * pre-release stores wipe via [Room.fallbackToDestructiveMigration]. If you ever
 * need to migrate live data, read the legacy keys here, seed the destination
 * tables, then delete the keys.
 */
@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            isForwardingEnabled    = prefs[Keys.IS_FORWARDING_ENABLED] ?: true,
            forwardingDelaySeconds = prefs[Keys.FORWARDING_DELAY_SECONDS] ?: 0,
            isBiometricLockEnabled = prefs[Keys.IS_BIOMETRIC_LOCK_ENABLED] ?: false,
            notificationsEnabled   = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            isFirstLaunch          = prefs[Keys.IS_FIRST_LAUNCH] ?: true,
        )
    }

    override suspend fun updateSettings(transform: suspend (AppSettings) -> AppSettings) {
        val current = settings.first()
        val updated = transform(current)
        dataStore.edit { prefs ->
            prefs[Keys.IS_FORWARDING_ENABLED]     = updated.isForwardingEnabled
            prefs[Keys.FORWARDING_DELAY_SECONDS]  = updated.forwardingDelaySeconds
            prefs[Keys.IS_BIOMETRIC_LOCK_ENABLED] = updated.isBiometricLockEnabled
            prefs[Keys.NOTIFICATIONS_ENABLED]     = updated.notificationsEnabled
            prefs[Keys.IS_FIRST_LAUNCH]           = updated.isFirstLaunch
        }
    }

    // ── Preference keys ───────────────────────────────────────────────────────

    private object Keys {
        val IS_FORWARDING_ENABLED     = booleanPreferencesKey("is_forwarding_enabled")
        val FORWARDING_DELAY_SECONDS  = intPreferencesKey("forwarding_delay_seconds")
        val IS_BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("is_biometric_lock_enabled")
        val NOTIFICATIONS_ENABLED     = booleanPreferencesKey("notifications_enabled")
        val IS_FIRST_LAUNCH           = booleanPreferencesKey("is_first_launch")
    }
}
