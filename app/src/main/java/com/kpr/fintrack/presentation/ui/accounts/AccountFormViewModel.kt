package com.kpr.fintrack.presentation.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kpr.fintrack.domain.model.Account
import com.kpr.fintrack.domain.model.AccountType
import com.kpr.fintrack.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountFormUiState(
    val id: Long = 0,
    val name: String = "",
    val nameError: String? = null,
    val accountType: AccountType = AccountType.SAVINGS,
    val isAccountTypeDropdownExpanded: Boolean = false,
    val bankName: String = "",
    val bankNameError: String? = null,
    val accountNumber: String = "",
    val accountNumberError: String? = null,
    val currentBalance: String = "0.0",
    val currentBalanceError: String? = null,
    val description: String = "",
    val color: String? = null,
    val isActive: Boolean = true,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isFormValid: Boolean = false
)

@HiltViewModel
class AccountFormViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountFormUiState())
    val uiState: StateFlow<AccountFormUiState> = _uiState.asStateFlow()

    init {
        validateForm()
    }

    fun loadAccount(accountId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            accountRepository.getAccountById(accountId).collect { account ->
                account?.let {
                    _uiState.update { state ->
                        state.copy(
                            id = account.id,
                            name = account.name,
                            accountType = account.accountType,
                            bankName = account.bankName,
                            accountNumber = account.accountNumber,
                            currentBalance = account.currentBalance.toString(),
                            description = account.description ?: "",
                            color = account.color,
                            isActive = account.isActive,
                            isLoading = false
                        )
                    }
                    validateForm()
                }
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update { 
            it.copy(
                name = name,
                nameError = if (name.isBlank()) "Name cannot be empty" else null
            )
        }
        validateForm()
    }

    fun updateAccountType(accountType: AccountType) {
        _uiState.update { it.copy(accountType = accountType) }
        validateForm()
    }

    fun setAccountTypeDropdownExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isAccountTypeDropdownExpanded = expanded) }
    }

    fun updateBankName(bankName: String) {
        _uiState.update { 
            it.copy(
                bankName = bankName,
                bankNameError = if (bankName.isBlank()) "Bank name cannot be empty" else null
            )
        }
        validateForm()
    }

    fun updateAccountNumber(accountNumber: String) {
        _uiState.update { 
            it.copy(
                accountNumber = accountNumber,
                accountNumberError = when {
                    accountNumber.isBlank() -> "Account number cannot be empty"
                    accountNumber.length < 4 -> "Account number must be at least 4 digits"
                    !accountNumber.all { it.isDigit() } -> "Account number must contain only digits"
                    else -> null
                }
            )
        }
        validateForm()
    }

    fun updateCurrentBalance(balance: String) {
        _uiState.update { 
            it.copy(
                currentBalance = balance,
                currentBalanceError = try {
                    balance.toDouble()
                    null
                } catch (e: NumberFormatException) {
                    "Invalid balance amount"
                }
            )
        }
        validateForm()
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
        validateForm()
    }

    fun updateIsActive(isActive: Boolean) {
        _uiState.update { it.copy(isActive = isActive) }
        validateForm()
    }

    fun saveAccount() {
        val currentState = _uiState.value
        
        if (!currentState.isFormValid) return
        
        _uiState.update { it.copy(isSaving = true) }
        
        viewModelScope.launch {
            try {
                val account = Account(
                    id = currentState.id,
                    name = currentState.name,
                    accountType = currentState.accountType,
                    bankName = currentState.bankName,
                    accountNumber = currentState.accountNumber,
                    currentBalance = currentState.currentBalance.toDoubleOrNull() ?: 0.0,
                    description = currentState.description.takeIf { it.isNotBlank() },
                    color = currentState.color,
                    isActive = currentState.isActive
                )
                
                if (currentState.id > 0) {
                    accountRepository.updateAccount(account)
                } else {
                    accountRepository.insertAccount(account)
                }
                
                _uiState.update { it.copy(isSaving = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun validateForm() {
        val currentState = _uiState.value
        
        val isValid = currentState.nameError == null && currentState.name.isNotBlank() &&
                currentState.bankNameError == null && currentState.bankName.isNotBlank() &&
                currentState.accountNumberError == null && currentState.accountNumber.isNotBlank() &&
                currentState.currentBalanceError == null
        
        _uiState.update { it.copy(isFormValid = isValid) }
    }
}