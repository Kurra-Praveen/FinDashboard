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

data class DashboardUiState(
    val isLoading: Boolean = true,
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
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                val currentMonth = YearMonth.now()
                val startOfMonth = currentMonth.atDay(1).atStartOfDay()
                val endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59)

                val previousMonth = currentMonth.minusMonths(1)
                val startOfPreviousMonth = previousMonth.atDay(1).atStartOfDay()
                val endOfPreviousMonth = previousMonth.atEndOfMonth().atTime(23, 59, 59)

                // Get current month transactions
                transactionRepository.getTransactionsByDateRange(startOfMonth, endOfMonth)
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
                            transactionRepository.getTotalSpending(startOfPreviousMonth, endOfPreviousMonth)
                        } catch (e: Exception) {
                            BigDecimal.ZERO
                        }

                        val comparison = if (prevSpending > BigDecimal.ZERO) {
                            ((currentSpending - prevSpending) / prevSpending * BigDecimal(100)).toFloat()
                        } else 0f

                        val topCategories = currentMonthTransactions
                            .filter { it.isDebit }
                            .groupBy { it.category }
                            .map { (category, transactions) ->
                                CategorySpendingData(
                                    category = category,
                                    amount = transactions.sumOf { it.amount }
                                )
                            }
                            .sortedByDescending { it.amount }
                            .take(5)

                        DashboardUiState(
                            isLoading = false,
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
        _uiState.value = DashboardUiState(isLoading = true)
        loadDashboardData()
    }
}
