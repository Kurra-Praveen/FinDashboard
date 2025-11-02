package com.kpr.fintrack.presentation.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kpr.fintrack.domain.model.BudgetDetails
import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.repository.BudgetRepository
// 1. Import TransactionRepository
import com.kpr.fintrack.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    // 2. MODIFIED: Inject TransactionRepository, NOT CategoriesViewModel
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    // Dialog management
    private val _dialogState = MutableStateFlow<EditBudgetDialogState?>(null)
    val dialogState: StateFlow<EditBudgetDialogState?> = _dialogState.asStateFlow()

    // 1. Get the "Total Budget" details
    private val totalBudget: StateFlow<BudgetDetails?> =
        budgetRepository.getTotalBudgetDetails(_currentMonth.value)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 2. Get the list of *active* category budgets
    private val categoryBudgets: StateFlow<List<BudgetDetails>> =
        budgetRepository.getCategoryBudgetDetails(_currentMonth.value)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Get the master list of *all* categories
    // MODIFIED: Call the repository directly.
    // (Assuming 'getLiveCategories()' is the correct method name from your TransactionRepository)
    private val allCategories: StateFlow<List<Category>> =
        transactionRepository.getAllCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Combine them into a single UI state (This logic remains the same)
    val uiState: StateFlow<BudgetUiState> = combine(
        totalBudget,
        categoryBudgets,
        allCategories
    ) { total, categoryBudgets, allCategories ->

        // Map all categories to a displayable item
        val categoryBudgetItems = allCategories.map { category ->
            // Find if a budget exists for this category
            val budgetDetails = categoryBudgets.find { it.budget.categoryId == category.id }
            CategoryBudgetUiItem(
                category = category,
                budgetDetails = budgetDetails
            )
        }

        BudgetUiState.Success(
            totalBudgetDetails = total,
            categoryBudgetItems = categoryBudgetItems
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetUiState.Loading
    )

    fun onSaveBudget(amountString: String) {
        val state = _dialogState.value ?: return
        val amount = amountString.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val categoryId = state.categoryId ?: return
        viewModelScope.launch {
            budgetRepository.saveBudget(
                amount = amount,
                categoryId = categoryId, // Null for total, ID for category
                month = _currentMonth.value
            )
            _dialogState.value = null // Close dialog
        }
    }

    fun onDeleteBudget() {
        val state = _dialogState.value ?: return
        val budgetId = state.budgetId ?: return

        viewModelScope.launch {
            budgetRepository.deleteBudget(budgetId)
            _dialogState.value = null // Close dialog
        }
    }

    fun showEditDialog(category: Category, existingBudget: BudgetDetails?) {
        _dialogState.value = EditBudgetDialogState(
            categoryId = category.id,
            categoryName = category.name,
            existingAmount = existingBudget?.budget?.amount,
            budgetId = existingBudget?.budget?.id
        )
    }

    fun dismissDialog() {
        _dialogState.value = null
    }
}

// --- UI State & Dialog State (These remain unchanged) ---

sealed interface BudgetUiState {
    data object Loading : BudgetUiState
    data class Success(
        val totalBudgetDetails: BudgetDetails?,
        val categoryBudgetItems: List<CategoryBudgetUiItem>
    ) : BudgetUiState
}

data class CategoryBudgetUiItem(
    val category: Category,
    val budgetDetails: BudgetDetails? // Null if no budget is set
)

data class EditBudgetDialogState(
    val categoryId: Long,
    val categoryName: String,
    val existingAmount: BigDecimal?,
    val budgetId: Long?
)