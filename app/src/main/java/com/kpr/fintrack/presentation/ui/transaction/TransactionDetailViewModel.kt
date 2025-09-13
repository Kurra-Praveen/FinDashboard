package com.kpr.fintrack.presentation.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.model.Transaction
import com.kpr.fintrack.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionDetailUiState(
    val isLoading: Boolean = true,
    val transaction: Transaction? = null,
    val error: String? = null
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    private var currentTransaction: Transaction? = null

    fun loadTransaction(transactionId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = TransactionDetailUiState(isLoading = true)

                // In a real implementation, you'd have a method to get transaction by ID
                // For now, we'll simulate it by getting all transactions and finding the one
                transactionRepository.getAllTransactions().collect { transactions ->
                    val transaction = transactions.find { it.id == transactionId }
                    currentTransaction = transaction

                    _uiState.value = TransactionDetailUiState(
                        isLoading = false,
                        transaction = transaction,
                        error = if (transaction == null) "Transaction not found" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = TransactionDetailUiState(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun updateCategory(category: Category) {
        currentTransaction?.let { transaction ->
            viewModelScope.launch {
                try {
                    val updatedTransaction = transaction.copy(
                        category = category,
                        updatedAt = java.time.LocalDateTime.now()
                    )
                    transactionRepository.updateTransaction(updatedTransaction)
                    currentTransaction = updatedTransaction

                    _uiState.value = _uiState.value.copy(
                        transaction = updatedTransaction
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to update category"
                    )
                }
            }
        }
    }

    fun updateNotes(notes: String) {
        currentTransaction?.let { transaction ->
            viewModelScope.launch {
                try {
                    val tags = notes.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val updatedTransaction = transaction.copy(
                        tags = tags,
                        updatedAt = java.time.LocalDateTime.now()
                    )
                    transactionRepository.updateTransaction(updatedTransaction)
                    currentTransaction = updatedTransaction

                    _uiState.value = _uiState.value.copy(
                        transaction = updatedTransaction
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to update notes"
                    )
                }
            }
        }
    }

    fun toggleVerification() {
        currentTransaction?.let { transaction ->
            viewModelScope.launch {
                try {
                    val updatedTransaction = transaction.copy(
                        isManuallyVerified = !transaction.isManuallyVerified,
                        updatedAt = java.time.LocalDateTime.now()
                    )
                    transactionRepository.updateTransaction(updatedTransaction)
                    currentTransaction = updatedTransaction

                    _uiState.value = _uiState.value.copy(
                        transaction = updatedTransaction
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to update verification"
                    )
                }
            }
        }
    }

    fun deleteTransaction() {
        currentTransaction?.let { transaction ->
            viewModelScope.launch {
                try {
                    transactionRepository.deleteTransaction(transaction)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to delete transaction"
                    )
                }
            }
        }
    }

    // Add this method to TransactionDetailViewModel class:

    fun saveChanges() {
        // This method can be used to batch save changes if needed
        // For now, individual changes are saved immediately
        // Could be extended to support batch updates in the future
    }

}
