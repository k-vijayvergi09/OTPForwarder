package com.samsung.android.otpforwarder.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extended (non-M3) semantic color tokens used by status pills, log rows and chart series.
 * Values verified against Figma file mMEjcCrdizgj7TMZJw8YZL (2026-04-22).
 *
 * Access via [OtpTheme.extendedColors] at call sites.
 */
@Immutable
data class ExtendedColors(
    // ── Success (green) — FORWARDED status ───────────────────────────────────
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,     // Figma: #CEF5E6
    val onSuccessContainer: Color,   // Figma: #00382A

    // ── Warning (orange) — PENDING / RETRY_QUEUED status ─────────────────────
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,     // Figma: #FFE0C0
    val onWarningContainer: Color,   // Figma: #3A1E00

    // ── Info (blue) — neutral informational ───────────────────────────────────
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
)

val LightExtendedColors = ExtendedColors(
    // Green — matches Figma "Forwarded" badge exactly
    success            = Color(0xFF006B3E),
    onSuccess          = Color(0xFFFFFFFF),
    successContainer   = Color(0xFFCEF5E6),
    onSuccessContainer = Color(0xFF00382A),

    // Orange — matches Figma "Pending" badge exactly
    warning            = Color(0xFF7A3F00),
    onWarning          = Color(0xFFFFFFFF),
    warningContainer   = Color(0xFFFFE0C0),
    onWarningContainer = Color(0xFF3A1E00),

    // Blue info
    info              = Color(0xFF006781),
    onInfo            = Color(0xFFFFFFFF),
    infoContainer     = Color(0xFFBAEAFF),
    onInfoContainer   = Color(0xFF001F29),
)

val DarkExtendedColors = ExtendedColors(
    success            = Color(0xFF72D9A8),
    onSuccess          = Color(0xFF00391F),
    successContainer   = Color(0xFF005235),
    onSuccessContainer = Color(0xFFCEF5E6),

    warning            = Color(0xFFFFA95C),
    onWarning          = Color(0xFF3F2000),
    warningContainer   = Color(0xFF5C2E00),
    onWarningContainer = Color(0xFFFFE0C0),

    info              = Color(0xFF5FD4FE),
    onInfo            = Color(0xFF003544),
    infoContainer     = Color(0xFF004D62),
    onInfoContainer   = Color(0xFFBAEAFF),
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
