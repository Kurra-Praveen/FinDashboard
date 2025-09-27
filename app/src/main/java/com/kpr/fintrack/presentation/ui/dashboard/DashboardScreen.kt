package com.kpr.fintrack.presentation.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.model.Transaction
import com.kpr.fintrack.presentation.ui.components.SpendingOverviewCard
import com.kpr.fintrack.presentation.ui.components.CategorySpendingCard
import com.kpr.fintrack.presentation.ui.components.RecentTransactionItem
import com.kpr.fintrack.utils.extensions.formatCurrency
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTransactions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onAddTransaction: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FinTrack",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToAccounts) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = "Accounts"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransaction,
                icon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add transaction"
                    )
                },
                text = { Text("Add") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { paddingValues ->

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Something went wrong",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    item {
                        SpendingOverviewCard(
                            monthlySpending = uiState.currentMonthSpending,
                            monthlyCredit = uiState.currentMonthCredit,
                            previousMonthComparison = uiState.previousMonthComparison
                        )
                    }
                    item {
                        AnalyticsPreviewCard(
                            categoryData = uiState.topCategories,
                            onViewAllAnalytics = onNavigateToAnalytics
                        )
                    }

                    if (uiState.topCategories.isNotEmpty()) {
                        item {
                            Text(
                                text = "Top Categories",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                items(uiState.topCategories) { categoryData ->
                                    CategorySpendingCard(
                                        category = categoryData.category,
                                        amount = categoryData.amount,
                                        onClick = {
                                            // Navigate to category transactions
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.recentTransactions.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Transactions",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )

                                TextButton(onClick = onNavigateToTransactions) {
                                    Text("View All")
                                }
                            }
                        }

                        items(uiState.recentTransactions,key = { transaction -> transaction.id }) { transaction ->
                            RecentTransactionItem(
                                transaction = transaction,
                                onClick = {
                                    // Navigate to transaction detail
                                    android.util.Log.d("DashboardScreen", "Recent transaction clicked: ${transaction.id}")
                                    onTransactionClick(transaction.id)
                                }
                            )
                        }
                    }

                    if (uiState.isEmpty) {
                        item {
                            EmptyStateCard(
                                onStartInboxScan = {
                                    viewModel.startInboxScan()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsPreviewCard(
    categoryData: List<CategorySpendingData>,
    onViewAllAnalytics: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spending Analytics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                TextButton(
                    onClick = {
                        android.util.Log.d("Analytics", "View Details clicked") // Debug log
                        onViewAllAnalytics() // ✅ Call the function with parentheses
                    }
                ){
                    Text("View Details")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick category preview
            if (categoryData.isNotEmpty()) {
                categoryData.take(3).forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            //Text(text =  Category.getDefaultCategories().find { x -> x.id==category.category.id}?.icon ?: "Unknown")
                            CategoryIcon(category.category.id)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Category.getDefaultCategories().find { x -> x.id==category.category.id}?.name ?: "Unknown",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Text(
                            text = category.amount.formatCurrency(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Text(
                    text = "No spending data available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@Composable
fun CategoryIcon(categoryId: Long) {
    val context = LocalContext.current
    val category = Category.getDefaultCategories().find { it.id == categoryId }

    category?.let {
        // Try to resolve as drawable first
        val resId = context.resources.getIdentifier(it.icon, "drawable", context.packageName)

        if (resId != 0) {
            // ✅ Found drawable → show image
            Image(
                painter = painterResource(id = resId),
                contentDescription = it.name,
                modifier = Modifier.size(24.dp)
            )
        } else {
            // ❌ Not a drawable → treat it as emoji / text
            Text(
                text = it.icon,
                fontSize = 20.sp,
                modifier = Modifier.size(24.dp)
            )
        }
    } ?: Text("Unknown")
}


@Composable
private fun EmptyStateCard(
    onStartInboxScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No Transactions Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Start by scanning your SMS inbox to import existing transactions, or wait for new SMS messages to be automatically detected.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onStartInboxScan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan SMS Inbox")
            }
        }
    }
}
