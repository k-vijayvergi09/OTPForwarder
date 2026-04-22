package com.samsung.android.otpforwarder.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * TODO(designsystem): The Figma uses Inter + JetBrains Mono. Once the font files
 * are bundled under res/font/, swap [FontFamily.Default] / [FontFamily.Monospace]
 * for the actual families.
 */
private val DisplayFontFamily = FontFamily.Default
private val BodyFontFamily = FontFamily.Default
val MonoFontFamily: FontFamily = FontFamily.Monospace

private val baseline = Typography()

val OtpTypography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = DisplayFontFamily),
    displayMedium = baseline.displayMedium.copy(fontFamily = DisplayFontFamily),
    displaySmall = baseline.displaySmall.copy(fontFamily = DisplayFontFamily),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = DisplayFontFamily),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = DisplayFontFamily),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = DisplayFontFamily),
    titleLarge = baseline.titleLarge.copy(fontFamily = DisplayFontFamily),
    titleMedium = baseline.titleMedium.copy(fontFamily = BodyFontFamily),
    titleSmall = baseline.titleSmall.copy(fontFamily = BodyFontFamily),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = BodyFontFamily),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = BodyFontFamily),
    bodySmall = baseline.bodySmall.copy(fontFamily = BodyFontFamily),
    labelLarge = baseline.labelLarge.copy(fontFamily = BodyFontFamily),
    labelMedium = baseline.labelMedium.copy(fontFamily = BodyFontFamily),
    labelSmall = baseline.labelSmall.copy(fontFamily = BodyFontFamily),
)

/**
 * Monospace style used for OTP codes, tokens, logs.
 */
val OtpCodeTextStyle: TextStyle = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    letterSpacing = 2.sp,
)
