package com.kpr.fintrack.presentation.ui.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.repository.TransactionRepository
import com.kpr.fintrack.utils.parsing.CategoryMatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    //private val transactionRepository: TransactionRepository
    private val categoryMatcher: CategoryMatcher
) : ViewModel() {
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    init {
        categoryMatcher.getLiveCategoriesFlow()
            .onEach { list -> _categories.value = list }
            .launchIn(viewModelScope)
    }

    // Helper to get current snapshot
    fun currentCategories(): List<Category> = _categories.value
}

