package com.samsung.android.otpforwarder.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * App-wide theme. Wire this around every screen root so both the M3
 * color scheme and our [ExtendedColors] are available under
 * [androidx.compose.runtime.CompositionLocal]s.
 */
@Composable
fun OtpForwarderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OtpTypography,
            shapes = OtpShapes,
            content = content,
        )
    }
}

/**
 * Accessor that mirrors MaterialTheme-style call sites (`OtpTheme.extendedColors`).
 */
object OtpTheme {
    val extendedColors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}
