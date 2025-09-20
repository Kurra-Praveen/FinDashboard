package com.kpr.fintrack.presentation.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kpr.fintrack.presentation.ui.analystics.AnalyticsViewModel // ✅ Fixed typo: analytics not analystics
import com.kpr.fintrack.presentation.ui.components.charts.CategoryPieChart
import com.kpr.fintrack.presentation.ui.components.charts.MonthlySpendingChart
import com.kpr.fintrack.presentation.ui.components.charts.WeeklySpendingChart
import com.kpr.fintrack.presentation.ui.components.StatCard
import com.kpr.fintrack.utils.extensions.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // ✅ Add debug effect to see what's happening
    LaunchedEffect(uiState) {
        android.util.Log.d("AnalyticsScreen", "UI State: isLoading=${uiState.isLoading}, error=${uiState.error}, hasData=${uiState.analyticsSummary != null}")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Analytics",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading analytics...")
                    }
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
                            text = "Error loading analytics",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            android.util.Log.d("AnalyticsScreen", "Retry button clicked")
                            viewModel.refresh()
                        }) {
                            Text("Retry")
                        }
                    }
                }
            }

            // ✅ Check for data more explicitly
            uiState.analyticsSummary != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Summary Stats
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                title = "Avg Daily",
                                value = uiState.analyticsSummary!!.averageDailySpending.formatCurrency(),
                                modifier = Modifier.weight(1f)
                            )

                            StatCard(
                                title = "Highest Day",
                                value = uiState.analyticsSummary!!.highestSpendingDay.formatCurrency(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Monthly Spending Chart
                    if (uiState.analyticsSummary?.monthlyData?.isNotEmpty() == true) {
                        item {
                            MonthlySpendingChart(monthlyData = uiState.analyticsSummary!!.monthlyData)
                        }
                    }

                    // Category Pie Chart
                    item {
                        CategoryPieChart(
                            categoryData = uiState.analyticsSummary!!.categoryData
                        )
                    }

                    // Weekly Spending Chart
                    item {
                        WeeklySpendingChart(
                            weeklyData = uiState.analyticsSummary!!.weeklyData
                        )
                    }
                }
            }

            // ✅ Add fallback case for no data
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📊",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No analytics data available",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add some transactions to see analytics",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
