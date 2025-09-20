package com.kpr.fintrack.presentation.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kpr.fintrack.presentation.navigation.Screen

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
            label = { Text("Dashboard") },
            selected = currentRoute == Screen.Dashboard.route,
            onClick = { onNavigate(Screen.Dashboard.route) }
        )
        
        NavigationBarItem(
            icon = { Icon(Icons.Default.Payments, contentDescription = "Transactions") },
            label = { Text("Transactions") },
            selected = currentRoute == Screen.Transactions.route,
            onClick = { onNavigate(Screen.Transactions.route) }
        )
        
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Accounts") },
            label = { Text("Accounts") },
            selected = currentRoute == Screen.Accounts.route,
            onClick = { onNavigate(Screen.Accounts.route) }
        )
        
        NavigationBarItem(
            icon = { Icon(Icons.Default.Analytics, contentDescription = "Analytics") },
            label = { Text("Analytics") },
            selected = currentRoute == Screen.Analytics.route,
            onClick = { onNavigate(Screen.Analytics.route) }
        )
    }
}