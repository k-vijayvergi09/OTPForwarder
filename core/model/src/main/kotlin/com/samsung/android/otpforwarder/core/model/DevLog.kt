package com.samsung.android.otpforwarder.core.model

import kotlinx.datetime.Instant

// ── Status ────────────────────────────────────────────────────────────────────

enum class DevLogStatus {
    /** Operation completed as expected. */
    OK,
    /** Operation completed but with a non-fatal anomaly. */
    WARN,
    /** Operation failed or was blocked. */
    ERROR,
}

// ── Stage ─────────────────────────────────────────────────────────────────────

/**
 * Each stage corresponds to one discrete step in the SMS-to-forwarding pipeline.
 * The [label] is used in the exported .txt log file.
 */
enum class DevLogStage(val label: String) {
    SMS_RECEIVED          ("SMS Received"),
    PERMISSION_CHECK      ("Permission Check"),
    SMS_PARSED            ("SMS Parsed"),
    OTP_DETECTION         ("OTP Detection"),
    MONITOR_RECORDED      ("Monitor Recorded"),
    NOTIFICATION_TRIGGERED("Notification Triggered"),
    REPOSITORY_RECORDED   ("Repository Recorded"),
    EVENT_BUS_EMITTED     ("Event Bus Emitted"),
    WORKER_DISPATCHED     ("Worker Dispatched"),
    FORWARDING_STARTED    ("Forwarding Started"),
    SETTINGS_CHECKED      ("Settings Checked"),
    DESTINATIONS_LOADED   ("Destinations Loaded"),
    SMS_DISPATCH          ("SMS Dispatch"),
    EMAIL_DISPATCH        ("Email Dispatch"),
    FORWARDING_COMPLETE   ("Forwarding Complete"),
}

// ── Entry ─────────────────────────────────────────────────────────────────────

/**
 * A single timestamped log entry for one pipeline step.
 *
 * @param timestamp When this step was executed.
 * @param stage     Which pipeline stage produced this entry.
 * @param status    Whether the step succeeded, warned, or failed.
 * @param message   Short human-readable description of what happened.
 * @param detail    Optional extra context (e.g. "eventId=abc123, emitted=true").
 */
data class DevLogEntry(
    val timestamp: Instant,
    val stage: DevLogStage,
    val status: DevLogStatus,
    val message: String,
    val detail: String? = null,
)

// ── Log ───────────────────────────────────────────────────────────────────────

/**
 * The full trace of pipeline steps for a single OTP detection event.
 *
 * Entries are ordered chronologically (oldest first).
 */
data class DevLog(
    val eventId: String,
    val entries: List<DevLogEntry> = emptyList(),
)
