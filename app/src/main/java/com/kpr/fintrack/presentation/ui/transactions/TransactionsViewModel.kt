package com.kpr.fintrack.presentation.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val transactions: List<Transaction> = emptyList(),
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

    // cache the last fetched (filtered, unsorted) transactions so sorting can be applied in-memory
    private val _cachedFilteredTransactions = MutableStateFlow<List<Transaction>>(emptyList())

    private fun logFilterValues(filter: TransactionFilter) {
        android.util.Log.d(
            "FilterDebug",
            "Applying filters:\n\tCategories: ${filter.categoryIds}\n\tTransaction Type: ${filter.isDebit}\n\tMin Amount: ${filter.minAmount}\n\tMax Amount: ${filter.maxAmount}\n\tStart Date: ${filter.startDate}\n\tEnd Date: ${filter.endDate}"
        )
    }

    private fun refreshFilteredTransactions() {
        val filter = _currentFilter.value
        val search = _searchQuery.value
        logFilterValues(filter)
        viewModelScope.launch {
            try {
                val result = if (search.isNotBlank() || hasActiveFilter(filter)) {
                    transactionRepository.getFilteredTransactions(
                        filter.copy(searchQuery = search.takeIf { it.isNotBlank() })
                    ).first()
                } else {
                    transactionRepository.getAllTransactions().first()
                }

                android.util.Log.d("TransactionsViewModel", "Fetched ${result.size} transactions from repository")
                _cachedFilteredTransactions.value = result
            } catch (e: Exception) {
                android.util.Log.e("TransactionsViewModel", "Error fetching filtered transactions", e)
            }
        }
    }

    // init moved below property declarations to ensure flows are initialized before use
    init {
        android.util.Log.d("TransactionsViewModel", "ViewModel initialized")
        // initially load all transactions into the cache
        refreshFilteredTransactions()
    }

    // uiState now combines search/filter/sort and cached list. Sorting applied in-memory on the cached list.
    val uiState: StateFlow<TransactionsUiState> = combine(
        _searchQuery,
        _currentFilter,
        _currentSort,
        _cachedFilteredTransactions
    ) { searchQuery, filter, sort, cached ->
        try {
            val sorted = applyInMemorySort(cached, sort)

            TransactionsUiState(
                isLoading = false,
                transactions = sorted,
                searchQuery = searchQuery,
                currentFilter = filter,
                hasActiveFilters = hasActiveFilter(filter) || searchQuery.isNotBlank(),
                activeFilterCount = countActiveFilters(filter, searchQuery),
                currentSort = sort
            )
        } catch (exception: Exception) {
            TransactionsUiState(
                isLoading = false,
                error = exception.message ?: "Unknown error occurred",
                currentFilter = filter
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )


    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        // refresh cache based on new search
        refreshFilteredTransactions()
    }

    fun onSearch(query: String) {
        _searchQuery.value = query
        refreshFilteredTransactions()
    }

    fun applyFilter(filter: TransactionFilter) {
        android.util.Log.d("TransactionsViewModel", "applyFilter called with: $filter")
        _currentFilter.value = filter
        refreshFilteredTransactions()
    }

    fun clearAllFilters() {
        _currentFilter.value = TransactionFilter()
        _searchQuery.value = ""
        refreshFilteredTransactions()
    }

    fun refresh() {
        // manual refresh to re-query repository
        refreshFilteredTransactions()
    }

    fun applySort(sortType: SortType) {
        android.util.Log.d("TransactionsViewModel", "applySort called with: $sortType")
        _currentSort.value = sortType
        // sorting is applied in the uiState combine using cached list, no DB hit required
    }

    private fun applyInMemorySort(list: List<Transaction>, sort: SortType): List<Transaction> {
        return when (sort) {
            SortType.AMOUNT_ASC -> list.sortedBy { it.amount }
            SortType.AMOUNT_DESC -> list.sortedByDescending { it.amount }
            SortType.DATE_ASC -> list.sortedBy { it.date }
            SortType.DATE_DESC -> list.sortedByDescending { it.date }
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
