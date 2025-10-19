package com.kpr.fintrack.presentation.ui.accounts

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kpr.fintrack.domain.model.Account
import com.kpr.fintrack.domain.model.Transaction
import com.kpr.fintrack.domain.repository.AccountRepository
import com.kpr.fintrack.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountDetailUiState(
    val account: Account? = null,
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountDetailUiState())
    val uiState: StateFlow<AccountDetailUiState> = _uiState.asStateFlow()

    private val _transactions = MutableStateFlow<Flow<PagingData<Transaction>>>(emptyFlow())
    val transactions: StateFlow<Flow<PagingData<Transaction>>> = _transactions.asStateFlow()

    fun loadAccount(accountId: Long) {
        Log.d("AccountDetailVM", "loadAccount called for accountId=$accountId")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            accountRepository.getAccountById(accountId)
                .catch { e ->
                    Log.e("AccountDetailVM", "Error loading account for id=$accountId: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
                .collect { account ->
                    Log.d("AccountDetailVM", "Account loaded for id=$accountId: ${account?.name}")
                    _uiState.value = _uiState.value.copy(
                        account = account,
                        isLoading = false
                    )
                }
        }
    }

    fun loadAccountTransactions(accountId: Long) {
        Log.d("AccountDetailVM", "loadAccountTransactions called for accountId=$accountId")
        viewModelScope.launch {
            try {
                val flow = transactionRepository.getPaginatedTransactionsByAccountId(accountId)
                    .cachedIn(viewModelScope)
                Log.d("AccountDetailVM", "Setting transactions Flow for accountId=$accountId")
                _transactions.value = flow
            } catch (e: Exception) {
                Log.e("AccountDetailVM", "Failed to load transactions for accountId=$accountId: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.value.account?.let { account ->
                accountRepository.deleteAccount(account)
            }
        }
    }
}
