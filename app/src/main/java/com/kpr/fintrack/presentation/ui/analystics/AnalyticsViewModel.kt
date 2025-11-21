package com.kpr.fintrack.presentation.ui.analystics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kpr.fintrack.domain.model.AnalyticsSummary
import com.kpr.fintrack.domain.model.CategorySpendingData
import com.kpr.fintrack.domain.model.MonthlySpendingData
import com.kpr.fintrack.domain.model.WeeklySpendingData
import com.kpr.fintrack.domain.model.TopMerchantData
import com.kpr.fintrack.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale
import javax.inject.Inject
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val selectedTimeRange: AnalyticsTimeRange = AnalyticsTimeRange.THIS_MONTH,
    val analyticsSummary: AnalyticsSummary? = null,
    val error: String? = null
)

enum class AnalyticsTimeRange {
    THIS_MONTH,
    LAST_30_DAYS
}

@Stable
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        android.util.Log.d("AnalyticsViewModel", "ViewModel initialized")
        loadAnalytics(range = _uiState.value.selectedTimeRange)
    }

    private fun loadAnalytics(range: AnalyticsTimeRange) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // --- 4. ADD DATE CALCULATION LOGIC ---
                val (startOfPeriod, endOfPeriod) = when (range) {
                    AnalyticsTimeRange.THIS_MONTH -> {
                        val currentMonth = YearMonth.now()
                        Pair(
                            currentMonth.atDay(1).atStartOfDay(),
                            currentMonth.atEndOfMonth().atTime(23, 59, 59)
                        )
                    }
                    AnalyticsTimeRange.LAST_30_DAYS -> {
                        val end = LocalDateTime.now()
                        val start = end.minusDays(30).with(LocalTime.MIN)
                        Pair(start, end)
                    }
                }

                // --- 5. PASS DATES TO THE REPOSITORY ---
                val summary = transactionRepository.getAnalyticsSummary(
                    startDate = startOfPeriod,
                    endDate = endOfPeriod
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        analyticsSummary = summary,
                        selectedTimeRange = range
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Unknown error")
                }
            }
        }
    }

    // ✅ Safe analytics creation with proper error handling
    private suspend fun createSafeAnalyticsSummary(): AnalyticsSummary {
        return try {
            // Try to get real data from repository
            val realSummary = transactionRepository.getAnalyticsSummary(LocalDateTime.now(),
                LocalDateTime.now())
            android.util.Log.d("AnalyticsViewModel", "Got real analytics summary")
            realSummary
        } catch (e: Exception) {
            android.util.Log.w("AnalyticsViewModel", "Failed to get real data, using fallback: ${e.message}")

            // ✅ Fallback to safe mock data
            createMockAnalyticsSummary()
        }
    }

    // ✅ Safe mock data for testing
    private fun createMockAnalyticsSummary(): AnalyticsSummary {
        val currentMonth = YearMonth.now()
        val formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())

        val monthlyData = try {
            (0..5).map { i ->
                val month = currentMonth.minusMonths(i.toLong())
                MonthlySpendingData(
                    yearMonth = month,
                    monthName = month.format(formatter),
                    totalSpent = BigDecimal("${(i + 1) * 1000}"),
                    totalIncome = BigDecimal("${(i + 1) * 1500}"),
                    netAmount = BigDecimal("${(i + 1) * 500}")
                )
            }.reversed()
        } catch (e: Exception) {
            android.util.Log.w("AnalyticsViewModel", "Error creating monthly data: ${e.message}")
            emptyList()
        }

        val categoryData = listOf(
            CategorySpendingData("Food", "🍕", BigDecimal("2000"), 40f, 25,color = null),
            CategorySpendingData("Transportation", "🚗", BigDecimal("1500"), 30f, 15,null),
            CategorySpendingData("Shopping", "🛒", BigDecimal("1000"), 20f, 10,null),
            CategorySpendingData("Bills", "💡", BigDecimal("500"), 10f, 5,null)
        )

        val weeklyData = (1..4).map { week ->
            WeeklySpendingData(
                weekNumber = week,
                weekRange = "Week $week",
                amount = BigDecimal("${week * 300}")
            )
        }

        val topMerchants = listOf(
            TopMerchantData("Amazon", BigDecimal("800"), 5),
            TopMerchantData("Flipkart", BigDecimal("600"), 3),
            TopMerchantData("Swiggy", BigDecimal("400"), 8)
        )

        return AnalyticsSummary(
            monthlyData = monthlyData,
            categoryData = categoryData,
            weeklyData = weeklyData,
            topMerchants = topMerchants,
            averageDailySpending = BigDecimal("165.50"),
            highestSpendingDay = BigDecimal("850.00"),
            mostUsedCategory = "Food"
        )
    }

    fun refresh() {
        android.util.Log.d("AnalyticsViewModel", "Refresh requested")
        loadAnalytics(range = _uiState.value.selectedTimeRange)
    }

    // --- 6. ADD THIS NEW FUNCTION ---
    fun setTimeRange(range: AnalyticsTimeRange) {
        if (range == _uiState.value.selectedTimeRange && !_uiState.value.isLoading) return

        // Load data for the new range
        loadAnalytics(range = range)
    }
}
