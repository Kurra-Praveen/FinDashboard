package com.kpr.fintrack.presentation.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kpr.fintrack.domain.model.BudgetDetails
import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.model.Transaction
import com.kpr.fintrack.domain.repository.BudgetRepository
import com.kpr.fintrack.domain.repository.TransactionRepository
import com.kpr.fintrack.services.scanner.InboxScannerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject

import java.time.LocalTime
import com.kpr.fintrack.utils.FinTrackLogger

enum class DashboardTimeRange {
    THIS_MONTH,
    LAST_30_DAYS
}
data class DashboardUiState(
    val isLoading: Boolean = true,
    val totalBudgetDetails: BudgetDetails? = null, // This field is correct
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
    private val inboxScannerManager: InboxScannerManager,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val TAG = "DashboardViewModel"

    init {
        FinTrackLogger.d(TAG, "ViewModel initialized")
    }

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData(range = _uiState.value.selectedTimeRange)
    }

    private fun loadDashboardData(range: DashboardTimeRange = DashboardTimeRange.THIS_MONTH) {
        FinTrackLogger.d(TAG, "Loading dashboard data for range: $range")
        viewModelScope.launch {
            try {
                // (Date calculations remain the same)
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

                // MODIFIED: 1. Define all flows to be combined
                // Flow for current period transactions
                val currentMonthTransactionsFlow = transactionRepository.getTransactionsByDateRange(startOfPeriod, endOfPeriod)
                // Flow for recent transactions
                val recentTransactionsFlow = transactionRepository.getRecentTransactions(10)
                // MODIFIED: 2. Create a budget flow that respects the selected time range
                val totalBudgetFlow: Flow<BudgetDetails?> = if (range == DashboardTimeRange.THIS_MONTH) {
                    // Only fetch budget if we are in "This Month" view
                    budgetRepository.getTotalBudgetDetails(YearMonth.now())
                } else {
                    // Otherwise, emit null so the budget card hides
                    flowOf(null)
                }

                FinTrackLogger.d(TAG, "Combining flows for dashboard data.")
                // MODIFIED: 3. Combine all THREE flows
                combine(
                    currentMonthTransactionsFlow,
                    recentTransactionsFlow,
                    totalBudgetFlow
                ) { currentMonthTransactions, recentTransactions, budget -> // MODIFIED: 4. Added 'budget'

                    // (All your existing calculations remain identical)
                    val currentSpending = currentMonthTransactions
                        .filter { it.isDebit }
                        .sumOf { it.amount }

                    val currentCredits = currentMonthTransactions
                        .filter { !it.isDebit }
                        .sumOf { it.amount }

                    val prevSpending = try {
                        transactionRepository.getTotalSpending(startOfPreviousPeriod, endOfPreviousPeriod)
                    } catch (e: Exception) {
                        FinTrackLogger.e(TAG, "Error getting previous month spending", e)
                        BigDecimal.ZERO
                    }

                    val comparison = if (prevSpending > BigDecimal.ZERO) {
                        ((currentSpending - prevSpending) / prevSpending * BigDecimal(100)).toFloat()
                    } else 0f

                    val topCategories = currentMonthTransactions
                        .filter { it.isDebit }
                        .groupBy { it.category }
                        .map { (_, transactions) ->
                            val category = transactions.first().category
                            CategorySpendingData(
                                category = category,
                                amount = transactions.sumOf { it.amount }
                            )
                        }
                        .sortedByDescending { it.amount }
                        .take(5)

                    FinTrackLogger.d(TAG, "Dashboard data combined. Current spending: $currentSpending, Budget: $budget")
                    // MODIFIED: 5. Pass the 'budget' object to the UI state
                    DashboardUiState(
                        isLoading = false,
                        totalBudgetDetails = budget, // <-- HERE is the integration
                        selectedTimeRange = range,
                        currentMonthSpending = currentSpending,
                        currentMonthCredit = currentCredits,
                        previousMonthComparison = comparison,
                        topCategories = topCategories,
                        recentTransactions = recentTransactions,
                        isEmpty = recentTransactions.isEmpty()
                        // 'error' is null here, which is correct for a successful emission
                    )
                }
                    .catch { exception ->
                        FinTrackLogger.e(TAG, "Error in dashboard data flow: ${exception.message}", exception)
                        // MODIFIED: 6. Corrected the 'catch' block to update the state
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "Unknown error occurred"
                            )
                        }
                    }
                    .collect { state ->
                        _uiState.value = state
                    }

            } catch (e: Exception) {
                FinTrackLogger.e(TAG, "Error loading dashboard data: ${e.message}", e)
                _uiState.value = DashboardUiState(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun startInboxScan() {
        FinTrackLogger.d(TAG, "Starting inbox scan.")
        viewModelScope.launch {
            inboxScannerManager.startInboxScan()
        }
    }

    fun refresh() {
        FinTrackLogger.d(TAG, "Refreshing dashboard data.")
        _uiState.value = _uiState.value.copy(isLoading = true) // MODIFIED: Use copy for safety
        // Pass the current range to reload
        loadDashboardData(range = _uiState.value.selectedTimeRange)
    }

    fun setTimeRange(range: DashboardTimeRange) {
        FinTrackLogger.d(TAG, "Setting time range to: $range")
        // If the user taps the same button, don't reload
        if (range == _uiState.value.selectedTimeRange && !_uiState.value.isLoading) {
            FinTrackLogger.d(TAG, "Time range already selected or loading, skipping reload.")
            return
        }

        // Set loading state and update the selected range
        _uiState.update {
            it.copy(isLoading = true, selectedTimeRange = range)
        }

        // Load data for the new range
        loadDashboardData(range = range)
    }
}