package com.kpr.fintrack.presentation.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kpr.fintrack.R
import com.kpr.fintrack.domain.model.Account
import com.kpr.fintrack.presentation.theme.CreditColor
import com.kpr.fintrack.presentation.theme.DebitColor
import com.kpr.fintrack.presentation.ui.components.AccountItem
import com.kpr.fintrack.presentation.ui.components.EmptyStateMessage
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
                title = { Text(stringResource(id = R.string.accounts_screen_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.content_description_back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAccount) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.content_description_add_account)
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
                    message = stringResource(id = R.string.accounts_empty_state_message),
                    subMessage = stringResource(id = R.string.accounts_empty_state_sub_message),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Monthly Summary
                    item {
                        MonthlySummaryCard(
                            totalInflow = uiState.totalInflow,
                            totalOutflow = uiState.totalOutflow,
                            netFlow = uiState.netFlow
                        )
                    }
                    
                    // Account List with Analytics
                    items(uiState.accounts) { account ->
                        val accountAnalytics = uiState.accountAnalytics[account.id]
                        AccountItemWithAnalytics(
                            account = account,
                            accountAnalytics = accountAnalytics,
                            onClick = { onAccountClick(account.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlySummaryCard(
    totalInflow: BigDecimal,
    totalOutflow: BigDecimal,
    netFlow: BigDecimal
) {
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
                text = stringResource(id = R.string.this_month),
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MonthlySummaryItem(
                    label = stringResource(id = R.string.inflow),
                    value = totalInflow,
                    color = CreditColor,
                    modifier = Modifier.weight(1f)
                )
                
                MonthlySummaryItem(
                    label = stringResource(id = R.string.outflow),
                    value = totalOutflow,
                    color = DebitColor,
                    modifier = Modifier.weight(1f)
                )
                
                MonthlySummaryItem(
                    label = stringResource(id = R.string.net_flow),
                    value = netFlow,
                    color = if (netFlow >= BigDecimal.ZERO) CreditColor else DebitColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MonthlySummaryItem(
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
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AccountItemWithAnalytics(
    account: com.kpr.fintrack.domain.model.Account,
    accountAnalytics: Account.MonthlyAnalytics?,
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
            if (accountAnalytics != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AnalyticsItem(
                        label = stringResource(id = R.string.inflow),
                        value = accountAnalytics.totalInflow,
                        color = CreditColor,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsItem(
                        label = stringResource(id = R.string.outflow),
                        value = accountAnalytics.totalOutflow,
                        color = DebitColor,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsItem(
                        label = stringResource(id = R.string.net_flow),
                        value = accountAnalytics.netFlow,
                        color = if (accountAnalytics.isPositive) CreditColor else DebitColor,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Transaction count
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.transactions_this_month, accountAnalytics.transactionCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
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