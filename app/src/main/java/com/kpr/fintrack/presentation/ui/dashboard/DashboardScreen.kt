package com.kpr.fintrack.presentation.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.kpr.fintrack.domain.model.Transaction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.AdsClick
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
import com.kpr.fintrack.presentation.ui.components.SpendingOverviewCard
import com.kpr.fintrack.presentation.ui.components.CategorySpendingCard
import com.kpr.fintrack.presentation.ui.components.RecentTransactionItem
import com.kpr.fintrack.presentation.ui.components.CategoryIcon
import com.kpr.fintrack.presentation.ui.shared.CategoriesViewModel
import com.kpr.fintrack.presentation.ui.shared.LocalCategories
import com.kpr.fintrack.utils.extensions.formatCurrency
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import com.kpr.fintrack.domain.model.BudgetDetails
import com.kpr.fintrack.presentation.ui.budget.AnimatedProgressIndicator
import com.kpr.fintrack.utils.FormatUtils
import com.kpr.fintrack.utils.FinTrackLogger
import com.kpr.fintrack.presentation.theme.cardEntranceAnimation
import com.kpr.fintrack.presentation.theme.listItemEntranceAnimation
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTransactions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onAddTransaction: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val TAG = "DashboardScreen"
    FinTrackLogger.d(TAG, "DashboardScreen composable entered.")

    val uiState by viewModel.uiState.collectAsState()
    val categoriesViewModel: CategoriesViewModel = hiltViewModel()
    val categories by categoriesViewModel.categories.collectAsState()

    CompositionLocalProvider(LocalCategories provides categories) {
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
                        IconButton(onClick = onNavigateToBudgets) {
                            Icon(
                                Icons.Rounded.AdsClick,
                                contentDescription = "Budgets"
                            )
                        }
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
                    FinTrackLogger.d(TAG, "Dashboard is loading...")
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    FinTrackLogger.e(TAG, "Dashboard error: ${uiState.error}")
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
                    FinTrackLogger.d(TAG, "Dashboard content displayed. UI State: $uiState")
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            TimeRangeSelector(
                                selectedRange = uiState.selectedTimeRange,
                                onRangeSelected = { viewModel.setTimeRange(it) }
                            )
                        }

                        item {
                            SpendingOverviewCard(
                                monthlySpending = uiState.currentMonthSpending,
                                monthlyCredit = uiState.currentMonthCredit,
                                previousMonthComparison = uiState.previousMonthComparison,
                                modifier = Modifier.cardEntranceAnimation(delay = 0)
                            )
                        }
                        item {
                            // This card will only appear if a total budget is set
                            // It will use smooth animations to appear and disappear
                            AnimatedVisibility(
                                visible = uiState.totalBudgetDetails != null,
                                enter = fadeIn(animationSpec = spring()) + expandVertically(
                                    animationSpec = spring()
                                ),
                                exit = fadeOut(animationSpec = spring()) + shrinkVertically(
                                    animationSpec = spring()
                                )
                            ) {
                                // We check null again because of the exit animation
                                uiState.totalBudgetDetails?.let { details ->
                                    FinTrackLogger.d(TAG, "Total budget details: $details")
                                    TotalBudgetSummaryCard(
                                        details = details,
                                        formattedSpent = uiState.formattedBudgetSpent,
                                        formattedTotal = uiState.formattedBudgetTotal,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .cardEntranceAnimation(delay = 100)
                                    )
                                }
                            }
                        }
                        item {
                            FinTrackLogger.d(
                                TAG,
                                "Analytics preview category data: ${uiState.topCategories}"
                            )
                            AnalyticsPreviewCard(
                                categoryData = uiState.topCategories,
                                allCategories = categories,
                                onViewAllAnalytics = onNavigateToAnalytics,
                                modifier = Modifier.cardEntranceAnimation(delay = 200)
                            )
                        }

                        if (uiState.topCategories.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Top Categories (${if (uiState.selectedTimeRange == DashboardTimeRange.THIS_MONTH) "This Month" else "Last 30 Days"})",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    itemsIndexed(
                                        items = uiState.topCategories,
                                        key = { _, item -> item.category.id }
                                    ) { index, categoryData ->
                                        CategorySpendingCard(
                                            category = categoryData.category,
                                            amount = categoryData.amount,
                                            onClick = {
                                                // Navigate to category transactions
                                            },
                                            modifier = Modifier.listItemEntranceAnimation(index)
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

                            itemsIndexed(
                                items = uiState.recentTransactions,
                                key = { _, transaction -> transaction.id }
                            ) { index, transaction ->
                                RecentTransactionItem(
                                    transaction = transaction,
                                    onClick = {
                                        onTransactionClick(transaction.id)
                                    },
                                    modifier = Modifier.listItemEntranceAnimation(index)
                                )
                            }
                        }

                        if (uiState.isEmpty) {
                            item {
                                FinTrackLogger.d(TAG, "Empty state card displayed.")
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
}
@Composable
private fun TotalBudgetSummaryCard(
    details: BudgetDetails,
    formattedSpent: String,
    formattedTotal: String,
    modifier: Modifier = Modifier
) {
    FinTrackLogger.d("TotalBudgetSummaryCard", "Displaying budget details: $details")
    val percentage = (details.progress * 100).toInt()
    val percentageLeft = (100 - percentage).coerceAtLeast(0)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize() // Smoothly animates text changes
        ) {
            Text(
                text = "Monthly Budget",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            // Use the animated progress bar we built in Milestone 3
            AnimatedProgressIndicator(
                progress = details.progress,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // Spent vs Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spent: $formattedSpent",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (details.isOverspent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "of $formattedTotal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Percentage Left
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (details.isOverspent) "Overspent!" else "$percentageLeft% remaining",
                style = MaterialTheme.typography.bodySmall,
                color = if (details.isOverspent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRangeSelector(
    selectedRange: DashboardTimeRange,
    onRangeSelected: (DashboardTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        DashboardTimeRange.THIS_MONTH to "This Month",
        DashboardTimeRange.LAST_30_DAYS to "Last 30 Days"
    )

    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, (range, label) ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                onClick = { onRangeSelected(range) },
                selected = (range == selectedRange)
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun AnalyticsPreviewCard(
    categoryData: List<CategorySpendingData>,
    onViewAllAnalytics: () -> Unit,
    allCategories: List<Category>,
    modifier: Modifier = Modifier
) {
    FinTrackLogger.d(
        "AnalyticsPreviewCard",
        "Displaying analytics preview with category data: $categoryData"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
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
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(
                    onClick = {
                        android.util.Log.d("Analytics", "View Details clicked")
                        onViewAllAnalytics()
                    }
                ) {
                    Text("View Details")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                            CategoryIcon(category.category.id)
                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = allCategories.find { x -> x.id == category.category.id }?.name
                                    ?: "Unknown",
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
} // <-- THIS WAS MISSING ⭐


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

