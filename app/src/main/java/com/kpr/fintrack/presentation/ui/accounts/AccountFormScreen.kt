package com.kpr.fintrack.presentation.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kpr.fintrack.domain.model.Account
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormScreen(
    accountId: Long? = null,
    onNavigateBack: () -> Unit,
    viewModel: AccountFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    LaunchedEffect(accountId) {
        if (accountId != null && accountId > 0) {
            viewModel.loadAccount(accountId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (accountId == null) "Add Account" else "Edit Account") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { 
                            viewModel.saveAccount()
                            onNavigateBack()
                        },
                        enabled = uiState.isFormValid
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save"
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
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Account name field
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { viewModel.updateName(it) },
                        label = { Text("Account Name") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.nameError != null,
                        supportingText = { 
                            uiState.nameError?.let { Text(it) }
                        }
                    )
                    
                    // Account type dropdown
                    ExposedDropdownMenuBox(
                        expanded = uiState.isAccountTypeDropdownExpanded,
                        onExpandedChange = { viewModel.setAccountTypeDropdownExpanded(it) }
                    ) {
                        OutlinedTextField(
                            value = uiState.accountType.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Account Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isAccountTypeDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = uiState.isAccountTypeDropdownExpanded,
                            onDismissRequest = { viewModel.setAccountTypeDropdownExpanded(false) }
                        ) {
                            Account.AccountType.entries.forEach { accountType ->
                                DropdownMenuItem(
                                    text = { Text(accountType.name) },
                                    onClick = {
                                        viewModel.updateAccountType(accountType)
                                        viewModel.setAccountTypeDropdownExpanded(false)
                                    }
                                )
                            }
                        }
                    }
                    
                    // Bank name field
                    OutlinedTextField(
                        value = uiState.bankName,
                        onValueChange = { viewModel.updateBankName(it) },
                        label = { Text("Bank Name") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.bankNameError != null,
                        supportingText = { 
                            uiState.bankNameError?.let { Text(it) }
                        }
                    )
                    
                    // Account number field
                    OutlinedTextField(
                        value = uiState.accountNumber,
                        onValueChange = { viewModel.updateAccountNumber(it) },
                        label = { Text("Account Number (Last 4 digits)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.accountNumberError != null,
                        supportingText = { 
                            uiState.accountNumberError?.let { Text(it) }
                            ?: Text("Only the last 4 digits will be displayed")
                        }
                    )
                    
                    // Current balance field
                    OutlinedTextField(
                        value = uiState.currentBalance,
                        onValueChange = { viewModel.updateCurrentBalance(it) },
                        label = { Text("Current Balance") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = uiState.currentBalanceError != null,
                        supportingText = { 
                            uiState.currentBalanceError?.let { Text(it) }
                        }
                    )
                    
                    // Description field
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.updateDescription(it) },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5
                    )
                    
                    // Active status switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active Account")
                        Switch(
                            checked = uiState.isActive,
                            onCheckedChange = { viewModel.updateIsActive(it) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            if (uiState.isSaving) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}