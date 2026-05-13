package com.samsung.android.otpforwarder.core.network

/**
 * Result of a single email send or connection-test attempt.
 */
sealed class EmailSendResult {
    data object Success : EmailSendResult()
    data class Failure(val reason: String, val cause: Throwable? = null) : EmailSendResult()
}
