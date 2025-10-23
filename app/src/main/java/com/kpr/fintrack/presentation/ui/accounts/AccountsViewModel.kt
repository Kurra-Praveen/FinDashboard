package com.kpr.fintrack.presentation.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kpr.fintrack.domain.model.Account
import com.kpr.fintrack.domain.repository.AccountRepository
import com.kpr.fintrack.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.YearMonth
import javax.inject.Inject

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val totalInflow: BigDecimal = BigDecimal.ZERO,
    val totalOutflow: BigDecimal = BigDecimal.ZERO,
    val netFlow: BigDecimal = BigDecimal.ZERO,
    val accountAnalytics: Map<Long, Account.MonthlyAnalytics> = emptyMap()
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
                    _uiState.value = _uiState.value.copy(
                        accounts = accounts,
                        isLoading = false
                    )
                    
                    // Load monthly analytics for each account
                    loadMonthlyAnalytics(accounts)
                }
        }
    }
    
    private fun loadMonthlyAnalytics(accounts: List<Account>) {
        viewModelScope.launch {
            if (accounts.isEmpty()) {
                return@launch
            }
            
            val currentMonth = YearMonth.now()
            val accountAnalytics = mutableMapOf<Long, Account.MonthlyAnalytics>()
            var totalInflow = BigDecimal.ZERO
            var totalOutflow = BigDecimal.ZERO
            
            accounts.forEach { account ->
                try {
                    val analytics = accountRepository.getAccountMonthlyAnalytics(account.id, currentMonth)
                    accountAnalytics[account.id] = analytics
                    totalInflow = totalInflow.add(analytics.totalInflow)
                    totalOutflow = totalOutflow.add(analytics.totalOutflow)
                } catch (e: Exception) {
                    android.util.Log.e("AccountsViewModel", "Failed to load analytics for account ${account.id}", e)
                }
            }
            
            val netFlow = totalInflow.subtract(totalOutflow)
            
            _uiState.value = _uiState.value.copy(
                accountAnalytics = accountAnalytics,
                totalInflow = totalInflow,
                totalOutflow = totalOutflow,
                netFlow = netFlow
            )
        }
    }
}