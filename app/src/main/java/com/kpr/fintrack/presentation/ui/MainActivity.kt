package com.kpr.fintrack.presentation.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.kpr.fintrack.presentation.navigation.FinTrackNavigation
import com.kpr.fintrack.presentation.navigation.Screen
import com.kpr.fintrack.presentation.theme.FinTrackTheme
import com.kpr.fintrack.presentation.ui.components.BottomNavBar
import com.kpr.fintrack.presentation.ui.components.PermissionRequestScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FinTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val permissionsState = rememberMultiplePermissionsState(
                        permissions = listOf(
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.READ_SMS,
                            Manifest.permission.POST_NOTIFICATIONS,
                            //Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE
                        )
                    )

                    LaunchedEffect(Unit) {
                        permissionsState.launchMultiplePermissionRequest()
                    }

                    when {
                        permissionsState.allPermissionsGranted -> {
                            FinTrackApp()
                        }
                        else -> {
                            PermissionRequestScreen(
                                permissionsState = permissionsState,
                                onPermissionResult = { /* Handle permission result */ }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinTrackApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: Screen.Dashboard.route
    
    // Check if current screen is a main screen that should show bottom navigation
    val showBottomNav = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.Transactions.route,
        Screen.Accounts.route,
        Screen.Analytics.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomNav) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // Pop up to the start destination of the graph to avoid building up
                            // a large stack of destinations
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        FinTrackNavigation(
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}
