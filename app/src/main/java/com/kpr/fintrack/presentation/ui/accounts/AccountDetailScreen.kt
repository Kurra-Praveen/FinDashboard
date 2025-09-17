package com.kpr.fintrack.presentation.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kpr.fintrack.presentation.ui.components.AccountSummaryCard
import com.kpr.fintrack.presentation.ui.components.EmptyStateMessage
import com.kpr.fintrack.presentation.ui.components.TransactionItem
import com.kpr.fintrack.utils.extensions.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: Long,
    onNavigateBack: () -> Unit,
    onEditAccount: (Long) -> Unit,
    viewModel: AccountDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(accountId) {
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
                            imageVector = Icons.Default.ArrowBack,
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
                    
                    if (uiState.transactions.isEmpty()) {
                        item {
                            EmptyStateMessage(
                                message = "No transactions",
                                subMessage = "This account has no transactions yet"
                            )
                        }
                    } else {
                        items(uiState.transactions) { transaction ->
                            TransactionItem(
                                transaction = transaction,
                                onClick = { /* Navigate to transaction detail */ }
                            )
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