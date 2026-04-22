package com.samsung.android.otpforwarder.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Material 3 color tokens — verified against Figma file mMEjcCrdizgj7TMZJw8YZL (2026-04-22).
 * Primary seed: "trust blue" #2E5BFF.
 */

// ── Primary (trust blue) ──────────────────────────────────────────────────────
internal val Primary10  = Color(0xFF0A1F6B)   // onPrimaryContainer (light)
internal val Primary20  = Color(0xFF1A3399)
internal val Primary30  = Color(0xFF2244CC)
internal val Primary40  = Color(0xFF2E5BFF)   // primary (light)
internal val Primary80  = Color(0xFFBBC4FF)   // primary (dark)
internal val Primary90  = Color(0xFFDCE5FF)   // primaryContainer (light)
internal val Primary95  = Color(0xFFEEF1FF)
internal val Primary99  = Color(0xFFFCFBFF)   // background / surface
internal val Primary100 = Color(0xFFFFFFFF)

// ── Secondary (muted slate) ───────────────────────────────────────────────────
internal val Secondary10 = Color(0xFF151B2C)
internal val Secondary20 = Color(0xFF2A3142)
internal val Secondary30 = Color(0xFF40475A)
internal val Secondary40 = Color(0xFF585F73)
internal val Secondary80 = Color(0xFFBFC6DC)
internal val Secondary90 = Color(0xFFDCE2F9)

// ── Tertiary (amber accent) ───────────────────────────────────────────────────
internal val Tertiary10 = Color(0xFF2B1300)
internal val Tertiary20 = Color(0xFF472A00)
internal val Tertiary30 = Color(0xFF653F00)
internal val Tertiary40 = Color(0xFF845619)
internal val Tertiary80 = Color(0xFFFCB974)
internal val Tertiary90 = Color(0xFFFFDDB8)

// ── Error ─────────────────────────────────────────────────────────────────────
internal val Error10 = Color(0xFF410002)   // onErrorContainer (light)
internal val Error20 = Color(0xFF690005)
internal val Error30 = Color(0xFF93000A)
internal val Error40 = Color(0xFFBA1A1A)
internal val Error80 = Color(0xFFFFB4AB)
internal val Error90 = Color(0xFFFFDAD6)   // errorContainer (light)

// ── Neutrals ──────────────────────────────────────────────────────────────────
internal val Neutral0   = Color(0xFF000000)
internal val Neutral10  = Color(0xFF1B1B1F)   // onSurface (light)  — Figma #1b1b1f
internal val Neutral20  = Color(0xFF2F3035)
internal val Neutral90  = Color(0xFFE3E2E6)
internal val Neutral95  = Color(0xFFF1F0F5)   // nav bar bg          — Figma #f1f0f5
internal val Neutral99  = Color(0xFFFCFBFF)   // surface/background  — Figma #fcfbff
internal val Neutral100 = Color(0xFFFFFFFF)

// ── Neutral variants ──────────────────────────────────────────────────────────
internal val NeutralVariant30 = Color(0xFF45464F)  // onSurfaceVariant (light) — Figma #45464f
internal val NeutralVariant50 = Color(0xFF74777F)
internal val NeutralVariant60 = Color(0xFF8E9099)
internal val NeutralVariant80 = Color(0xFFC6C6D0)  // outlineVariant / divider  — Figma #c6c6d0
internal val NeutralVariant90 = Color(0xFFE1E2EC)
internal val NeutralVariant94 = Color(0xFFF6F5FA)  // surfaceVariant / card bg  — Figma #f6f5fa

// ── Color schemes ─────────────────────────────────────────────────────────────

val LightColorScheme = lightColorScheme(
    primary              = Primary40,
    onPrimary            = Primary100,
    primaryContainer     = Primary90,
    onPrimaryContainer   = Primary10,
    secondary            = Secondary40,
    onSecondary          = Primary100,
    secondaryContainer   = Secondary90,
    onSecondaryContainer = Secondary10,
    tertiary             = Tertiary40,
    onTertiary           = Primary100,
    tertiaryContainer    = Tertiary90,
    onTertiaryContainer  = Tertiary10,
    error                = Error40,
    onError              = Primary100,
    errorContainer       = Error90,
    onErrorContainer     = Error10,
    background           = Neutral99,
    onBackground         = Neutral10,
    surface              = Neutral99,
    onSurface            = Neutral10,
    surfaceVariant       = NeutralVariant94,
    onSurfaceVariant     = NeutralVariant30,
    outline              = NeutralVariant50,
    outlineVariant       = NeutralVariant80,
    inverseSurface       = Neutral20,
    inverseOnSurface     = Neutral95,
    inversePrimary       = Primary80,
    scrim                = Neutral0,
)

val DarkColorScheme = darkColorScheme(
    primary              = Primary80,
    onPrimary            = Primary20,
    primaryContainer     = Primary30,
    onPrimaryContainer   = Primary90,
    secondary            = Secondary80,
    onSecondary          = Secondary20,
    secondaryContainer   = Secondary30,
    onSecondaryContainer = Secondary90,
    tertiary             = Tertiary80,
    onTertiary           = Tertiary20,
    tertiaryContainer    = Tertiary30,
    onTertiaryContainer  = Tertiary90,
    error                = Error80,
    onError              = Error20,
    errorContainer       = Error30,
    onErrorContainer     = Error90,
    background           = Neutral10,
    onBackground         = Neutral90,
    surface              = Neutral10,
    onSurface            = Neutral90,
    surfaceVariant       = NeutralVariant30,
    onSurfaceVariant     = NeutralVariant80,
    outline              = NeutralVariant60,
    outlineVariant       = NeutralVariant30,
    inverseSurface       = Neutral90,
    inverseOnSurface     = Neutral20,
    inversePrimary       = Primary40,
    scrim                = Neutral0,
)
