package com.kpr.fintrack.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kpr.fintrack.presentation.ui.dashboard.DashboardScreen
import com.kpr.fintrack.presentation.ui.transactions.TransactionsScreen
import com.kpr.fintrack.presentation.ui.settings.SettingsScreen
import com.kpr.fintrack.presentation.ui.transaction.TransactionDetailScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Transactions : Screen("transactions")
    object Settings : Screen("settings")
    object TransactionDetail : Screen("transaction_detail/{transactionId}") {
        fun createRoute(transactionId: Long) = "transaction_detail/$transactionId"
    }
}

@Composable
fun FinTrackNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToTransactions = {
                    navController.navigate(Screen.Transactions.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onTransactionClick = { transactionId ->
                    // ✅ Add debug log here too
                    android.util.Log.d("Navigation", "Navigating to transaction: $transactionId")
                    navController.navigate("transaction_detail/$transactionId")
                }
            )
        }

        composable(Screen.Transactions.route) {
            TransactionsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.TransactionDetail.createRoute(transactionId))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(
                route = "transaction_detail/{transactionId}",
        arguments = listOf(
            navArgument("transactionId") {
                type = NavType.LongType
            }
        )
        ) { backStackEntry ->
        val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
            TransactionDetailScreen(
                transactionId = transactionId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
    }
    }
}
