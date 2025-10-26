package com.kpr.fintrack.presentation.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.LoadState
import com.kpr.fintrack.presentation.ui.components.AccountSummaryCard
import com.kpr.fintrack.presentation.ui.components.EmptyStateMessage
import android.util.Log
import com.kpr.fintrack.presentation.ui.components.RecentTransactionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: Long,
    onNavigateBack: () -> Unit,
    onEditAccount: (Long) -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: AccountDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // transactionsFlow is a Flow<PagingData<Transaction>> exposed by the ViewModel
    val transactionsFlow by viewModel.transactions.collectAsState()
    val transactions = transactionsFlow.collectAsLazyPagingItems()

    LaunchedEffect(accountId) {
        Log.d("AccountDetailScreen", "LaunchedEffect accountId=$accountId - loading account and transactions")
        viewModel.loadAccount(accountId)
        viewModel.loadAccountTransactions(accountId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.account?.name ?: "Account Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEditAccount(accountId) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Account"
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Account"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.account == null) {
                EmptyStateMessage(
                    message = "Account not found",
                    subMessage = "The requested account could not be found",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                when (val refreshState = transactions.loadState.refresh) {
                    is LoadState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is LoadState.Error -> {
                        val error = refreshState.error
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Error loading transactions",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = error.localizedMessage ?: "An unexpected error occurred",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { transactions.retry() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                    is LoadState.NotLoading -> {
                        if (transactions.itemCount == 0) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                EmptyStateMessage(
                                    message = "No transactions",
                                    subMessage = "This account has no transactions yet"
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    uiState.account?.let { account ->
                                        AccountSummaryCard(account = account)
                                    }
                                }

                                item {
                                    Text(
                                        text = "Recent Transactions",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }

                                // Paging items
                                items(count = transactions.itemCount) { index ->
                                    val transaction = transactions[index]
                                    transaction?.let { tx ->
                                        RecentTransactionItem(transaction = tx, onClick = { onTransactionClick(tx.id) })
                                    }
                                }

                                if (transactions.loadState.append is LoadState.Loading) {
                                    item {

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp), contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete this account? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount()
                        showDeleteDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}