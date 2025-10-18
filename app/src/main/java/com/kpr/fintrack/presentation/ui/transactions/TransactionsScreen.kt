package com.kpr.fintrack.presentation.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort

import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kpr.fintrack.presentation.ui.components.RecentTransactionItem
import com.kpr.fintrack.presentation.ui.components.AppSearchBar
import com.kpr.fintrack.presentation.ui.components.FilterBottomSheet
import com.kpr.fintrack.presentation.ui.components.SortType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateBack: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
        android.util.Log.d("TransactionsScreen", "Composable entered")
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (showSearchBar) {
                AppSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onActiveChange = { isActive ->
                        if (!isActive) showSearchBar = false
                    },
                    onSearch = viewModel::onSearch,
                    placeholder = "Search transactions..."
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = "Transactions",
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
                    },
                    actions = {
                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }

                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter"
                            )
                        }

                        // Sort button
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort"
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Amount ↑") },
                                    onClick = {
                                        viewModel.applySort(SortType.AMOUNT_ASC)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Amount ↓") },
                                    onClick = {
                                        viewModel.applySort(SortType.AMOUNT_DESC)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date ↑") },
                                    onClick = {
                                        viewModel.applySort(SortType.DATE_ASC)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date ↓") },
                                    onClick = {
                                        viewModel.applySort(SortType.DATE_DESC)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                )
            }
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
                            text = "Error loading transactions",
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

            uiState.transactions.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No transactions found",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (uiState.hasActiveFilters) {
                                "Try adjusting your filters"
                            } else {
                                "Transactions will appear here automatically"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    if (uiState.hasActiveFilters) {
                        item {
                            ActiveFiltersCard(
                                filterCount = uiState.activeFilterCount,
                                onClearFilters = viewModel::clearAllFilters
                            )
                        }
                    }

                    items(
                        items = uiState.transactions,
                        key = { it.id }
                    ) { transaction ->
                        RecentTransactionItem(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction.id) }
                        )
                    }
                }
            }
        }

        if (showFilterSheet) {
            FilterBottomSheet(
                currentFilter = uiState.currentFilter,
                onFilterUpdate = { filter ->
                    viewModel.applyFilter(filter)
                    showFilterSheet = false
                },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

@Composable
private fun ActiveFiltersCard(
    filterCount: Int,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$filterCount filter${if (filterCount != 1) "s" else ""} active",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            TextButton(onClick = onClearFilters) {
                Text(
                    text = "Clear All",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
