package com.samsung.android.otpforwarder.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ForwardToInbox
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed hierarchy of all navigation destinations in the app.
 * [route] is the string key used by Navigation Compose.
 *
 * The Rules destination was removed in the 2026-05-13 redesign — replaced with
 * a top-level Destinations list and two add-destination editor screens. The new
 * editor routes live under the Destinations parent so the bottom-nav active
 * state continues to highlight Destinations while the editor is on top of the
 * back stack.
 */
sealed class AppDestination(val route: String) {
    data object Onboarding           : AppDestination("onboarding")
    data object Home                 : AppDestination("home")
    data object Logs                 : AppDestination("logs")
    data object Destinations         : AppDestination("destinations")
    data object AddPhoneDestination  : AppDestination("destinations/add-phone")
    data object AddEmailDestination  : AppDestination("destinations/add-email")
    data object Settings             : AppDestination("settings")
    data object GmailSetup           : AppDestination("settings/gmail-setup")
}

/**
 * Bottom-navigation tabs (the three items always visible in the bottom bar).
 */
enum class BottomNavTab(
    val destination: AppDestination,
    val label: String,
    val icon: ImageVector,
) {
    HOME        (AppDestination.Home,         "Home",         Icons.Rounded.Home),
    DESTINATIONS(AppDestination.Destinations, "Destinations", Icons.Rounded.ForwardToInbox),
    LOGS        (AppDestination.Logs,         "Logs",         Icons.Rounded.History),
}
