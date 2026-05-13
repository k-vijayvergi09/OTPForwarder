package com.samsung.android.otpforwarder.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.samsung.android.otpforwarder.feature.destinations.AddEmailDestinationScreen
import com.samsung.android.otpforwarder.feature.destinations.AddPhoneDestinationScreen
import com.samsung.android.otpforwarder.feature.destinations.DestinationsScreen
import com.samsung.android.otpforwarder.feature.home.HomeScreen
import com.samsung.android.otpforwarder.feature.logs.LogsScreen
import com.samsung.android.otpforwarder.feature.onboarding.OnboardingScreen
import com.samsung.android.otpforwarder.feature.email.EmailScreen
import com.samsung.android.otpforwarder.feature.settings.SettingsScreen

@Composable
fun OtpNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination,
        modifier         = modifier,
    ) {
        composable(AppDestination.Onboarding.route) {
            OnboardingScreen(
                onNavigateToHome = {
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AppDestination.Home.route) {
            HomeScreen(
                onNavigateToLogs     = { navController.navigate(AppDestination.Logs.route) },
                onNavigateToSettings = { navController.navigate(AppDestination.Settings.route) },
            )
        }

        composable(AppDestination.Logs.route) {
            LogsScreen()
        }

        composable(AppDestination.Destinations.route) {
            DestinationsScreen(
                onAddPhone = { navController.navigate(AppDestination.AddPhoneDestination.route) },
                onAddEmail = { navController.navigate(AppDestination.AddEmailDestination.route) },
                onNavigateToSettings = { navController.navigate(AppDestination.Settings.route) }
            )
        }

        composable(AppDestination.AddPhoneDestination.route) {
            AddPhoneDestinationScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(AppDestination.AddEmailDestination.route) {
            AddEmailDestinationScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(AppDestination.Settings.route) {
            SettingsScreen(
                onNavigateBack         = { navController.popBackStack() },
                onNavigateToGmailSetup = { navController.navigate(AppDestination.GmailSetup.route) },
            )
        }

        composable(AppDestination.GmailSetup.route) {
            EmailScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
