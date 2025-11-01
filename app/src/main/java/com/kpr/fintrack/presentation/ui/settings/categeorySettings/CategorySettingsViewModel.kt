package com.kpr.fintrack.presentation.ui.settings.categeorySettings


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.repository.TransactionRepository
import com.kpr.fintrack.utils.parsing.CategoryMatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class CategorySettingsUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class CategorySettingsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryMatcher: CategoryMatcher // <-- We still need this!
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategorySettingsUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        transactionRepository.getAllCategories()
            .onEach { categories ->
                _uiState.update { it.copy(categories = categories, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            transactionRepository.deleteCategory(category)

            // --- CRITICAL LINK ---
            // We must invalidate the cache when we delete
            categoryMatcher.invalidateCache()
        }
    }
}