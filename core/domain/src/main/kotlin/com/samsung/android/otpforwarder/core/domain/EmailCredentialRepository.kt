package com.samsung.android.otpforwarder.core.domain

/**
 * Contract for persisting and retrieving the user's outbound Gmail credentials.
 *
 * Implementations must store credentials securely (e.g. EncryptedSharedPreferences).
 * The repository holds at most one credential set — saving again overwrites.
 */
interface EmailCredentialRepository {

    /** Returns the saved credentials, or null if none have been configured yet. */
    suspend fun get(): GmailCredentials?

    /** Persists [address] and [appPassword], overwriting any previously saved values. */
    suspend fun save(address: String, appPassword: String)

    /** Removes any stored credentials (e.g. when the user signs out / clears setup). */
    suspend fun clear()

    /** True if credentials have been saved, false otherwise. */
    suspend fun isConfigured(): Boolean = get() != null
}
