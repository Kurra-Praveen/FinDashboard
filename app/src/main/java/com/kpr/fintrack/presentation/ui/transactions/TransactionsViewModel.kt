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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
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

@Stable
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _currentFilter = MutableStateFlow(TransactionFilter())
    private val _currentSort = MutableStateFlow(SortType.DATE_DESC)

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: Flow<PagingData<Transaction>> = combine(
        _searchQuery,
        _currentFilter,
        _currentSort
    ) { searchQuery, filter, sort ->
        val finalFilter = filter.copy(
            searchQuery = searchQuery.ifBlank { null },
            sortOrder = sort.toSortOrderString()
        )
        android.util.Log.d("TransactionsViewModel", "Applying filter: $finalFilter")
        finalFilter
    }.flatMapLatest { filter ->
        transactionRepository.getFilteredTransactions(filter)
    }.cachedIn(viewModelScope)

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactions,
        _searchQuery,
        _currentFilter,
        _currentSort
    ) { transactionsData, searchQuery, filter, sort ->
        TransactionsUiState(
            isLoading = false,
            transactions = transactionsData,
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

    private fun SortType.toSortOrderString(): String {
        return when (this) {
            SortType.DATE_ASC -> "date_asc"
            SortType.DATE_DESC -> "date_desc"
            SortType.AMOUNT_ASC -> "amount_asc"
            SortType.AMOUNT_DESC -> "amount_desc"
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
