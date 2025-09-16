package com.kpr.fintrack.presentation.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kpr.fintrack.domain.model.Account
import com.kpr.fintrack.domain.use_case.AccountUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountUseCases: AccountUseCases
) : ViewModel() {

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        accountUseCases.getAccounts()
            .onEach { _accounts.value = it }
            .launchIn(viewModelScope)
    }

    fun addAccount(account: Account) {
        viewModelScope.launch {
            accountUseCases.addAccount(account)
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            accountUseCases.updateAccount(account)
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            accountUseCases.deleteAccount(account)
        }
    }
}
