package com.samsung.android.otpforwarder.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.RuleFolder
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed hierarchy of all navigation destinations in the app.
 * [route] is the string key used by Navigation Compose.
 */
sealed class AppDestination(val route: String) {
    data object Onboarding : AppDestination("onboarding")
    data object Home       : AppDestination("home")
    data object Logs       : AppDestination("logs")
    data object Rules      : AppDestination("rules")
    data object Settings   : AppDestination("settings")
}

/**
 * Bottom-navigation tabs (the three items always visible in the bottom bar).
 */
enum class BottomNavTab(
    val destination: AppDestination,
    val label: String,
    val icon: ImageVector,
) {
    HOME(AppDestination.Home,  "Home",  Icons.Rounded.Home),
    LOGS(AppDestination.Logs,  "Logs",  Icons.Rounded.History),
    RULES(AppDestination.Rules, "Rules", Icons.Rounded.RuleFolder),
}
