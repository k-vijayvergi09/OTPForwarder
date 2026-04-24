package com.samsung.android.otpforwarder

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.samsung.android.otpforwarder.core.designsystem.theme.OtpForwarderTheme
import com.samsung.android.otpforwarder.core.sms.IncomingSmsMonitor
import com.samsung.android.otpforwarder.navigation.AppDestination
import com.samsung.android.otpforwarder.navigation.BottomNavTab
import com.samsung.android.otpforwarder.navigation.OtpNavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "OtpForwarderMain"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var incomingSmsMonitor: IncomingSmsMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "MainActivity.onCreate")
        enableEdgeToEdge()
        setContent {
            OtpForwarderTheme {
                OtpForwarderApp(incomingSmsMonitor = incomingSmsMonitor)
            }
        }
    }
}

@Composable
private fun OtpForwarderApp(incomingSmsMonitor: IncomingSmsMonitor) {
    val navController       = rememberNavController()
    val navBackStackEntry   by navController.currentBackStackEntryAsState()
    val currentDestination  = navBackStackEntry?.destination
    val snackbarHostState   = remember { SnackbarHostState() }
    var lastHandledSequence by remember { mutableStateOf(0L) }

    val bottomBarRoutes = BottomNavTab.entries.map { it.destination.route }.toSet()
    val showBottomBar   = currentDestination?.route in bottomBarRoutes

    // Collect raw-SMS observations from the broadcast receiver and surface them
    // as a brief snackbar so the developer can verify the pipeline end-to-end.
    LaunchedEffect(incomingSmsMonitor) {
        Log.i(TAG, "Starting IncomingSmsMonitor collection")
        incomingSmsMonitor.latestObservation.collect { observation ->
            if (observation == null) {
                Log.i(TAG, "Observation is null, waiting for SMS")
                return@collect
            }
            Log.i(
                TAG,
                "Observed sequence=${observation.sequence} sender=${observation.message.sender} " +
                "lastHandled=$lastHandledSequence",
            )
            if (observation.sequence <= lastHandledSequence) {
                Log.i(TAG, "Skipping already handled sequence=${observation.sequence}")
                return@collect
            }
            lastHandledSequence = observation.sequence
            Log.i(TAG, "Showing snackbar for sender=${observation.message.sender}")
            snackbarHostState.showSnackbar(
                message = "Incoming SMS from ${observation.message.sender.ifBlank { "Unknown sender" }}",
            )
            Log.i(TAG, "Snackbar finished for sequence=${observation.sequence}")
        }
    }

    Scaffold(
        modifier     = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter   = slideInVertically(initialOffsetY = { it }),
                exit    = slideOutVertically(targetOffsetY = { it }),
            ) {
                NavigationBar {
                    BottomNavTab.entries.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == tab.destination.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick  = {
                                navController.navigate(tab.destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon  = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        OtpNavGraph(
            navController    = navController,
            // TODO M5: read isFirstLaunch from DataStore; for now always start at Home
            startDestination = AppDestination.Home.route,
            modifier         = Modifier.padding(innerPadding),
        )
    }
}
