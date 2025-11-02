package com.kpr.fintrack.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kpr.fintrack.presentation.ui.accounts.AccountDetailScreen
import com.kpr.fintrack.presentation.ui.accounts.AccountFormScreen
import com.kpr.fintrack.presentation.ui.accounts.AccountsScreen
import com.kpr.fintrack.presentation.ui.analystics.AnalyticsScreen
import com.kpr.fintrack.presentation.ui.budget.BudgetScreen
import com.kpr.fintrack.presentation.ui.dashboard.DashboardScreen
import com.kpr.fintrack.presentation.ui.settings.categeorySettings.CategoryFormScreen
import com.kpr.fintrack.presentation.ui.settings.categeorySettings.CategoryFormViewModel
import com.kpr.fintrack.presentation.ui.settings.categeorySettings.CategorySettingsScreen
import com.kpr.fintrack.presentation.ui.settings.categeorySettings.CategorySettingsViewModel
import com.kpr.fintrack.presentation.ui.transactions.TransactionsScreen
import com.kpr.fintrack.presentation.ui.settings.SettingsScreen
import com.kpr.fintrack.presentation.ui.settings.NotificationSettingsScreen
import com.kpr.fintrack.presentation.ui.transaction.AddTransactionScreen
import com.kpr.fintrack.presentation.ui.transaction.TransactionDetailScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Transactions : Screen("transactions")
    object Settings : Screen("settings")
    object Analytics : Screen("analytics")
    object Accounts : Screen("accounts")
    object AddAccount : Screen("add_account")
    object Budgets : Screen("budgets")
    object CategorySettings : Screen("categorySettings")

    object CategoryForm : Screen("categoryForm?categoryId={categoryId}") {
        // Helper function to build the route for either add or edit
        fun createRoute(categoryId: Long? = null): String {
            return if (categoryId != null) {
                "categoryForm?categoryId=$categoryId"
            } else {
                "categoryForm" // No ID means "add" mode
            }
        }
    }
    object TransactionDetail : Screen("transaction_detail/{transactionId}") {
        fun createRoute(transactionId: Long) = "transaction_detail/$transactionId"
    }
    object AccountDetail : Screen("account_detail/{accountId}") {
        fun createRoute(accountId: Long) = "account_detail/$accountId"
    }
    object EditAccount : Screen("edit_account/{accountId}") {
        fun createRoute(accountId: Long) = "edit_account/$accountId"
    }
    object NotificationSettings : Screen("notification_settings")
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
                },
                onNavigateToAnalytics = {
                    // ✅ Add analytics navigation
                    android.util.Log.d("Navigation", "Navigating to analytics")
                    navController.navigate(Screen.Analytics.route)
                },
                onNavigateToAccounts = {
                    android.util.Log.d("Navigation", "Navigating to accounts")
                    navController.navigate(Screen.Accounts.route)
                },
                onAddTransaction = {
                    navController.navigate("add_transaction")
                },
                onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) }
                    )
        }
        composable(Screen.Budgets.route) {
            BudgetScreen (
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("add_transaction") {
            AddTransactionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = "edit_transaction/{transactionId}",
            arguments = listOf(
                navArgument("transactionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
            AddTransactionScreen(
                transactionId = transactionId,
                onNavigateBack = {
                    navController.popBackStack()
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
        composable("analytics") {
            AnalyticsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.NotificationSettings.route)
                },
                onNavigateToCategorySettings = { // <-- ADD THIS
                    navController.navigate(Screen.CategorySettings.route)
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
        
        // Account screens
        composable(Screen.Accounts.route) {
            AccountsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onAccountClick = { accountId ->
                    navController.navigate(Screen.AccountDetail.createRoute(accountId))
                },
                onAddAccount = {
                    navController.navigate(Screen.AddAccount.route)
                }
            )
        }
        
        composable(
            route = Screen.AccountDetail.route,
            arguments = listOf(
                navArgument("accountId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: 0L
            AccountDetailScreen(
                accountId = accountId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEditAccount = { id ->
                    navController.navigate(Screen.EditAccount.createRoute(id))
                },
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.TransactionDetail.createRoute(transactionId))
                }
            )
        }
        
        composable(Screen.AddAccount.route) {
            AccountFormScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.EditAccount.route,
            arguments = listOf(
                navArgument("accountId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: 0L
            AccountFormScreen(
                accountId = accountId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Notification Settings Screen
        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.CategorySettings.route) {
            val viewModel = hiltViewModel<CategorySettingsViewModel>()
            val uiState by viewModel.uiState.collectAsState()

            CategorySettingsScreen(
                uiState = uiState,
                onNavigateBack = { navController.popBackStack() },
                onAddCategory = {
                    // Navigate to the form with NO ID
                    navController.navigate(Screen.CategoryForm.createRoute())
                },
                onEditCategory = { categoryId ->
                    // Navigate to the form WITH an ID
                    navController.navigate(Screen.CategoryForm.createRoute(categoryId))
                },
                onDeleteCategory = viewModel::deleteCategory
            )
        }

        // --- ADD THIS NEW DESTINATION FOR THE FORM ---
        composable(
            route = Screen.CategoryForm.route,
            arguments = listOf(navArgument("categoryId") {
                type = NavType.StringType // Use StringType for optional args
                nullable = true
            })
        ) {
            // We will create this VM and Screen next
            val viewModel = hiltViewModel<CategoryFormViewModel>()
            val uiState by viewModel.uiState.collectAsState()

            CategoryFormScreen (
                uiState = uiState,
                onNavigateBack = { navController.popBackStack() },
                onSave = {
                    viewModel.saveCategory()
                    navController.popBackStack() // Go back after save
                },
                onFormStateChanged = viewModel::onFormStateChanged,
                onKeywordAdded = viewModel::addKeywordToForm,
                onKeywordRemoved = viewModel::removeKeywordFromForm
            )
        }

    }
}
