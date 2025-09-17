package com.kpr.fintrack.presentation.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kpr.fintrack.presentation.ui.components.AccountItem
import com.kpr.fintrack.presentation.ui.components.EmptyStateMessage
import com.kpr.fintrack.presentation.ui.theme.Green
import com.kpr.fintrack.presentation.ui.theme.Red
import com.kpr.fintrack.utils.FormatUtils.formatAsCurrency
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onNavigateBack: () -> Unit,
    onAccountClick: (Long) -> Unit,
    onAddAccount: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAccount) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Account"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.accounts.isEmpty()) {
                EmptyStateMessage(
                    message = "No accounts found",
                    subMessage = "Add an account to get started",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Total Balance Summary
                    item {
                        AccountsSummaryCard(
                            totalBalance = uiState.totalBalance
                        )
                    }
                    
                    // Account List with Analytics
                    items(uiState.accounts) { account ->
                        val accountSummary = uiState.accountSummaries[account.id]
                        AccountItemWithAnalytics(
                            account = account,
                            accountSummary = accountSummary,
                            onClick = { onAccountClick(account.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccountsSummaryCard(totalBalance: BigDecimal) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = totalBalance.formatAsCurrency(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (totalBalance >= BigDecimal.ZERO) Green else Red
            )
        }
    }
}

@Composable
fun AccountItemWithAnalytics(
    account: com.kpr.fintrack.domain.model.Account,
    accountSummary: AccountSummary?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Account basic info
            AccountItem(
                account = account,
                onClick = onClick
            )
            
            // Analytics section
            if (accountSummary != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AnalyticsItem(
                        label = "Income",
                        value = accountSummary.totalInflow,
                        color = Green,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsItem(
                        label = "Expense",
                        value = accountSummary.totalOutflow,
                        color = Red,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsItem(
                        label = "Net Flow",
                        value = accountSummary.netFlow,
                        color = if (accountSummary.netFlow >= BigDecimal.ZERO) Green else Red,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun AnalyticsItem(
    label: String,
    value: BigDecimal,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.formatAsCurrency(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}