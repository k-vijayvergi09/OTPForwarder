package com.samsung.android.otpforwarder.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.samsung.android.otpforwarder.core.domain.EmailCredentialRepository
import com.samsung.android.otpforwarder.core.domain.GmailCredentials
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [EmailCredentialRepository] that stores Gmail credentials in
 * [EncryptedSharedPreferences] backed by AES-256-GCM (values) and AES-256-SIV (keys).
 *
 * Reads and writes are dispatched to [Dispatchers.IO] because the first call to
 * [EncryptedSharedPreferences.create] performs key-store I/O.
 */
@Singleton
class GmailCredentialStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : EmailCredentialRepository {

    /**
     * Lazy so the key-store initialisation happens off the main thread on first
     * access, not at injection time.
     */
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun get(): GmailCredentials? = withContext(Dispatchers.IO) {
        val address     = prefs.getString(KEY_ADDRESS, null)
        val appPassword = prefs.getString(KEY_APP_PASSWORD, null)
        if (address.isNullOrBlank() || appPassword.isNullOrBlank()) null
        else GmailCredentials(address = address, appPassword = appPassword)
    }

    override suspend fun save(address: String, appPassword: String) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(KEY_ADDRESS, address.trim())
            .putString(KEY_APP_PASSWORD, appPassword.replace(" ", ""))
            .apply()
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit()
            .remove(KEY_ADDRESS)
            .remove(KEY_APP_PASSWORD)
            .apply()
    }

    private companion object {
        const val PREFS_FILE      = "gmail_credentials"
        const val KEY_ADDRESS     = "gmail_address"
        const val KEY_APP_PASSWORD = "gmail_app_password"
    }
}
