package com.kpr.fintrack.presentation.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kpr.fintrack.domain.model.Transaction
import com.kpr.fintrack.domain.repository.TransactionFilter
import com.kpr.fintrack.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsUiState(
    val isLoading: Boolean = true,
    val transactions: List<Transaction> = emptyList(),
    val searchQuery: String = "",
    val currentFilter: TransactionFilter = TransactionFilter(),
    val error: String? = null,
    val hasActiveFilters: Boolean = false,
    val activeFilterCount: Int = 0
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
        init {
            android.util.Log.d("TransactionsViewModel", "ViewModel initialized")
        }

    private val _searchQuery = MutableStateFlow("")
    private val _currentFilter = MutableStateFlow(TransactionFilter())

    // In TransactionsViewModel.kt, update the uiState flow:

    val uiState: StateFlow<TransactionsUiState> = combine(
        _searchQuery,
        _currentFilter
    ) { searchQuery, filter ->
        try {
            val filteredTransactions = if (searchQuery.isNotBlank() || hasActiveFilter(filter)) {
                transactionRepository.getFilteredTransactions(
                    filter.copy(searchQuery = searchQuery.takeIf { it.isNotBlank() })
                ).first() // Use first() instead of await()
            } else {
                transactionRepository.getAllTransactions().first()
            }

            TransactionsUiState(
                isLoading = false,
                transactions = filteredTransactions,
                searchQuery = searchQuery,
                currentFilter = filter,
                hasActiveFilters = hasActiveFilter(filter) || searchQuery.isNotBlank(),
                activeFilterCount = countActiveFilters(filter, searchQuery)
            )
        } catch (exception: Exception) {
            TransactionsUiState(
                isLoading = false,
                error = exception.message ?: "Unknown error occurred"
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )


    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSearch(query: String) {
        _searchQuery.value = query
    }

    fun applyFilter(filter: TransactionFilter) {
        _currentFilter.value = filter
    }

    fun clearAllFilters() {
        _currentFilter.value = TransactionFilter()
        _searchQuery.value = ""
    }

    fun refresh() {
        // The StateFlow will automatically refresh when the repository data changes
        viewModelScope.launch {
            // Optionally trigger a manual refresh if needed
        }
    }

    private fun hasActiveFilter(filter: TransactionFilter): Boolean {
        return filter.categoryIds?.isNotEmpty() == true ||
                filter.startDate != null ||
                filter.endDate != null ||
                filter.minAmount != null ||
                filter.maxAmount != null ||
                filter.isDebit != null
    }

    private fun countActiveFilters(filter: TransactionFilter, searchQuery: String): Int {
        var count = 0
        if (filter.categoryIds?.isNotEmpty() == true) count++
        if (filter.startDate != null || filter.endDate != null) count++
        if (filter.minAmount != null || filter.maxAmount != null) count++
        if (filter.isDebit != null) count++
        if (searchQuery.isNotBlank()) count++
        return count
    }
}
