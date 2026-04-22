package com.samsung.android.otpforwarder.core.model

/**
 * Lifecycle state of a single OTP forwarding attempt.
 *
 * State machine:
 *   PENDING → FORWARDED        (success on first try)
 *   PENDING → FAILED           (terminal failure, no retries left)
 *   PENDING → RETRY_QUEUED     (transient failure, WorkManager will retry)
 *   RETRY_QUEUED → FORWARDED
 *   RETRY_QUEUED → FAILED
 */
enum class ForwardingStatus {
    /** Received and queued; forwarding not yet attempted. */
    PENDING,

    /** Successfully delivered to at least one destination. */
    FORWARDED,

    /** Terminal failure — all retry attempts exhausted. */
    FAILED,

    /** Transient failure; a WorkManager retry is scheduled. */
    RETRY_QUEUED,
}
