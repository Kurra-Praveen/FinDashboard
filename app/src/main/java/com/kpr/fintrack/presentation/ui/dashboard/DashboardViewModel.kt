package com.kpr.fintrack.presentation.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.model.Transaction
import com.kpr.fintrack.domain.repository.TransactionRepository
import com.kpr.fintrack.services.scanner.InboxScannerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject

import java.time.LocalTime // Add this import

enum class DashboardTimeRange {
    THIS_MONTH,
    LAST_30_DAYS
}
data class DashboardUiState(
    val isLoading: Boolean = true,
    val selectedTimeRange: DashboardTimeRange = DashboardTimeRange.THIS_MONTH,
    val currentMonthSpending: BigDecimal = BigDecimal.ZERO,
    val currentMonthCredit: BigDecimal = BigDecimal.ZERO,
    val previousMonthComparison: Float = 0f,
    val topCategories: List<CategorySpendingData> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val error: String? = null,
    val isEmpty: Boolean = false
)

data class CategorySpendingData(
    val category: Category,
    val amount: BigDecimal
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val inboxScannerManager: InboxScannerManager
) : ViewModel() {
        init {
            android.util.Log.d("DashboardViewModel", "ViewModel initialized")
        }

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData(range = _uiState.value.selectedTimeRange)
    }

    private fun loadDashboardData(range: DashboardTimeRange = DashboardTimeRange.THIS_MONTH) {
        viewModelScope.launch {
            try {
//                val currentMonth = YearMonth.now()
//                val startOfMonth = currentMonth.atDay(1).atStartOfDay()
//                val endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59)
//
//                val previousMonth = currentMonth.minusMonths(1)
//                val startOfPreviousMonth = previousMonth.atDay(1).atStartOfDay()
//                val endOfPreviousMonth = previousMonth.atEndOfMonth().atTime(23, 59, 59)

                // --- THIS IS THE KEY CHANGE ---
                // Calculate dates based on the selected range
                val (startOfPeriod, endOfPeriod) = when (range) {
                    DashboardTimeRange.THIS_MONTH -> {
                        val currentMonth = YearMonth.now()
                        Pair(
                            currentMonth.atDay(1).atStartOfDay(),
                            currentMonth.atEndOfMonth().atTime(23, 59, 59)
                        )
                    }
                    DashboardTimeRange.LAST_30_DAYS -> {
                        val end = LocalDateTime.now()
                        val start = end.minusDays(30).with(LocalTime.MIN)
                        Pair(start, end)
                    }
                }

                val (startOfPreviousPeriod, endOfPreviousPeriod) = when (range) {
                    DashboardTimeRange.THIS_MONTH -> {
                        val previousMonth = YearMonth.now().minusMonths(1)
                        Pair(
                            previousMonth.atDay(1).atStartOfDay(),
                            previousMonth.atEndOfMonth().atTime(23, 59, 59)
                        )
                    }
                    DashboardTimeRange.LAST_30_DAYS -> {
                        val end = startOfPeriod.minusSeconds(1)
                        val start = end.minusDays(30).with(LocalTime.MIN)
                        Pair(start, end)
                    }
                }
                // Get current month transactions
                transactionRepository.getTransactionsByDateRange(startOfPeriod, endOfPeriod)
                    .combine(
                        transactionRepository.getRecentTransactions(10)
                    ) { currentMonthTransactions, recentTransactions ->

                        // Calculate spending and credits for current month
                        val currentSpending = currentMonthTransactions
                            .filter { it.isDebit }
                            .sumOf { it.amount }

                        val currentCredits = currentMonthTransactions
                            .filter { !it.isDebit }
                            .sumOf { it.amount }

                        // Calculate previous month spending for comparison
                        val prevSpending = try {
                            transactionRepository.getTotalSpending(startOfPreviousPeriod, endOfPreviousPeriod)
                        } catch (e: Exception) {
                            BigDecimal.ZERO
                        }

                        val comparison = if (prevSpending > BigDecimal.ZERO) {
                            ((currentSpending - prevSpending) / prevSpending * BigDecimal(100)).toFloat()
                        } else 0f

                        val topCategories = currentMonthTransactions
                            .filter { it.isDebit }
                            .groupBy { it.category }
                            .map { (categoryId, transactions) ->
                                val category = transactions.first().category
                                CategorySpendingData(
                                    category = category,
                                    amount = transactions.sumOf { it.amount }
                                )
                            }
                            .sortedByDescending { it.amount }
                            .take(5)

                        DashboardUiState(
                            isLoading = false,
                            selectedTimeRange = range,
                            currentMonthSpending = currentSpending,
                            currentMonthCredit = currentCredits,
                            previousMonthComparison = comparison,
                            topCategories = topCategories,
                            recentTransactions = recentTransactions,
                            isEmpty = recentTransactions.isEmpty()
                        )
                    }
                    .catch { exception ->
                        emit(
                            DashboardUiState(
                                isLoading = false,
                                error = exception.message ?: "Unknown error occurred"
                            )
                        )
                    }
                    .collect { state ->
                        _uiState.value = state
                    }

            } catch (e: Exception) {
                _uiState.value = DashboardUiState(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun startInboxScan() {
        viewModelScope.launch {
            inboxScannerManager.startInboxScan()
        }
    }

    fun refresh() {
        _uiState.value = DashboardUiState(
            isLoading = true,
            selectedTimeRange = _uiState.value.selectedTimeRange // Keep the current range
        )
        // Pass the current range to reload
        loadDashboardData(range = _uiState.value.selectedTimeRange)
    }

    fun setTimeRange(range: DashboardTimeRange) {
        // If the user taps the same button, don't reload
        if (range == _uiState.value.selectedTimeRange && !_uiState.value.isLoading) return

        // Set loading state and update the selected range
        _uiState.update {
            it.copy(isLoading = true, selectedTimeRange = range)
        }

        // Load data for the new range
        loadDashboardData(range = range)
    }
}
