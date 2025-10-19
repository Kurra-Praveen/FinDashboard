package com.kpr.fintrack.presentation.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kpr.fintrack.domain.model.Transaction
import com.kpr.fintrack.domain.repository.TransactionFilter
import com.kpr.fintrack.domain.repository.TransactionRepository
import com.kpr.fintrack.presentation.ui.components.SortType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsUiState(
    val isLoading: Boolean = true,
    val transactions: PagingData<Transaction> = PagingData.empty(),
    val searchQuery: String = "",
    val currentFilter: TransactionFilter = TransactionFilter(),
    val error: String? = null,
    val hasActiveFilters: Boolean = false,
    val activeFilterCount: Int = 0,
    val currentSort: SortType = SortType.DATE_DESC
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _currentFilter = MutableStateFlow(TransactionFilter())
    private val _currentSort = MutableStateFlow(SortType.DATE_DESC)

    val transactions: StateFlow<PagingData<Transaction>> = transactionRepository
        .getPaginatedTransactions()
        .cachedIn(viewModelScope)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PagingData.empty())

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactions,
        _searchQuery,
        _currentFilter,
        _currentSort
    ) { transactions, searchQuery, filter, sort ->
        TransactionsUiState(
            isLoading = false,
            transactions = transactions,
            searchQuery = searchQuery,
            currentFilter = filter,
            hasActiveFilters = hasActiveFilter(filter) || searchQuery.isNotBlank(),
            activeFilterCount = countActiveFilters(filter, searchQuery),
            currentSort = sort
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionsUiState())


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
        // The Paging library handles refresh automatically.
    }

    fun applySort(sortType: SortType) {
        _currentSort.value = sortType
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
