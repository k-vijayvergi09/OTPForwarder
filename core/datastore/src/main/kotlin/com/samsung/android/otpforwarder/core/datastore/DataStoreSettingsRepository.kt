package com.samsung.android.otpforwarder.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.samsung.android.otpforwarder.core.domain.SettingsRepository
import com.samsung.android.otpforwarder.core.model.AppSettings
import com.samsung.android.otpforwarder.core.model.DestinationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SettingsRepository] backed by Jetpack [DataStore] with [Preferences].
 *
 * All keys are stable string constants — changing them is a breaking migration.
 * [DestinationType] lists are persisted as comma-separated enum names
 * (e.g. "SMS,EMAIL").  Unknown names are silently dropped so old data stays
 * compatible if we ever add new destination types.
 */
@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            isForwardingEnabled    = prefs[Keys.IS_FORWARDING_ENABLED] ?: true,
            forwardingDelaySeconds = prefs[Keys.FORWARDING_DELAY_SECONDS] ?: 0,
            defaultDestinations    = prefs[Keys.DEFAULT_DESTINATIONS]
                ?.parseDestinationTypes()
                ?: listOf(DestinationType.SMS),
            defaultPhoneNumber     = prefs[Keys.DEFAULT_PHONE_NUMBER] ?: "",
            defaultEmailAddress    = prefs[Keys.DEFAULT_EMAIL_ADDRESS] ?: "",
            isBiometricLockEnabled = prefs[Keys.IS_BIOMETRIC_LOCK_ENABLED] ?: false,
            notificationsEnabled   = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            isFirstLaunch          = prefs[Keys.IS_FIRST_LAUNCH] ?: true,
        )
    }

    override suspend fun updateSettings(transform: suspend (AppSettings) -> AppSettings) {
        // Read current state, apply the transform, then write each field atomically.
        val current = settings.first()
        val updated = transform(current)
        dataStore.edit { prefs ->
            prefs[Keys.IS_FORWARDING_ENABLED]     = updated.isForwardingEnabled
            prefs[Keys.FORWARDING_DELAY_SECONDS]  = updated.forwardingDelaySeconds
            prefs[Keys.DEFAULT_DESTINATIONS]      = updated.defaultDestinations.toPrefsString()
            prefs[Keys.DEFAULT_PHONE_NUMBER]      = updated.defaultPhoneNumber
            prefs[Keys.DEFAULT_EMAIL_ADDRESS]     = updated.defaultEmailAddress
            prefs[Keys.IS_BIOMETRIC_LOCK_ENABLED] = updated.isBiometricLockEnabled
            prefs[Keys.NOTIFICATIONS_ENABLED]     = updated.notificationsEnabled
            prefs[Keys.IS_FIRST_LAUNCH]           = updated.isFirstLaunch
        }
    }

    // ── Preference keys ───────────────────────────────────────────────────────

    private object Keys {
        val IS_FORWARDING_ENABLED     = booleanPreferencesKey("is_forwarding_enabled")
        val FORWARDING_DELAY_SECONDS  = intPreferencesKey("forwarding_delay_seconds")
        val DEFAULT_DESTINATIONS      = stringPreferencesKey("default_destinations")
        val DEFAULT_PHONE_NUMBER      = stringPreferencesKey("default_phone_number")
        val DEFAULT_EMAIL_ADDRESS     = stringPreferencesKey("default_email_address")
        val IS_BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("is_biometric_lock_enabled")
        val NOTIFICATIONS_ENABLED     = booleanPreferencesKey("notifications_enabled")
        val IS_FIRST_LAUNCH           = booleanPreferencesKey("is_first_launch")
    }
}

// ── Extension helpers ─────────────────────────────────────────────────────────

/** Serialize a [DestinationType] list to a comma-separated string, e.g. "SMS,EMAIL". */
private fun List<DestinationType>.toPrefsString(): String =
    joinToString(",") { it.name }

/** Deserialize a comma-separated string back to a [DestinationType] list.
 *  Unknown names are silently dropped for forward-compatibility. */
private fun String.parseDestinationTypes(): List<DestinationType> =
    split(",")
        .mapNotNull { token ->
            try { DestinationType.valueOf(token.trim()) }
            catch (_: IllegalArgumentException) { null }
        }
        .ifEmpty { listOf(DestinationType.SMS) }
