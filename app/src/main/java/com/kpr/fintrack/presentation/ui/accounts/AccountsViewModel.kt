package com.kpr.fintrack.presentation.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kpr.fintrack.domain.model.Account
import com.kpr.fintrack.domain.model.Transaction
import com.kpr.fintrack.domain.model.TransactionType
import com.kpr.fintrack.domain.repository.AccountRepository
import com.kpr.fintrack.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val totalBalance: BigDecimal = BigDecimal.ZERO,
    val accountSummaries: Map<Long, AccountSummary> = emptyMap()
)

data class AccountSummary(
    val totalInflow: BigDecimal = BigDecimal.ZERO,
    val totalOutflow: BigDecimal = BigDecimal.ZERO,
    val netFlow: BigDecimal = BigDecimal.ZERO
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
        loadAccountAnalytics()
    }

    fun loadAccounts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            accountRepository.getAllAccounts()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
                .collect { accounts ->
                    val totalBalance = accounts.fold(BigDecimal.ZERO) { acc, account ->
                        acc.add(account.currentBalance)
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        accounts = accounts,
                        totalBalance = totalBalance,
                        isLoading = false
                    )
                    
                    // Load transaction data for each account
                    loadAccountAnalytics()
                }
        }
    }
    
    private fun loadAccountAnalytics() {
        viewModelScope.launch {
            val accounts = _uiState.value.accounts
            if (accounts.isEmpty()) {
                return@launch
            }
            
            transactionRepository.getAllTransactions()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to load transaction data"
                    )
                }
                .collect { transactions ->
                    // Group transactions by account
                    val accountSummaries = accounts.associate { account ->
                        val accountTransactions = transactions.filter { it.account?.id == account.id }
                        
                        // Calculate inflows (income)
                        val totalInflow = accountTransactions
                            .filter { !it.isDebit }
                            .fold(BigDecimal.ZERO) { acc, transaction -> 
                                acc.add(transaction.amount) 
                            }
                            
                        // Calculate outflows (expense)
                        val totalOutflow = accountTransactions
                            .filter { it.isDebit }
                            .fold(BigDecimal.ZERO) { acc, transaction -> 
                                acc.add(transaction.amount) 
                            }
                            
                        // Calculate net flow
                        val netFlow = totalInflow.subtract(totalOutflow)
                        
                        // Create account summary
                        account.id to AccountSummary(
                            totalInflow = totalInflow,
                            totalOutflow = totalOutflow,
                            netFlow = netFlow
                        )
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        accountSummaries = accountSummaries
                    )
                }
        }
    }
}