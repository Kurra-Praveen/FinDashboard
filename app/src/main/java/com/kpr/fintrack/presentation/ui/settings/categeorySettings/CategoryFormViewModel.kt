package com.kpr.fintrack.presentation.ui.settings.categeorySettings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.repository.TransactionRepository
import com.kpr.fintrack.utils.parsing.BankUtils
import com.kpr.fintrack.utils.parsing.CategoryMatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryFormUiState(
    val form: CategoryFormData = CategoryFormData(),
    val defaultIcons: List<String> = BankUtils.CategoryIconDefaults.defaultIcons,
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false
)

data class CategoryFormData(
    val id: Long? = null,
    val name: String = "",
    val icon: String = BankUtils.CategoryIconDefaults.defaultIcons.first(),
    val color: String = "#FF0000", // We can add a color picker later
    val keywords: List<String> = emptyList(),
    val currentKeywordInput: String = ""
)

// Helper to map from domain model to form state
fun Category.toFormData(): CategoryFormData = CategoryFormData(
    id = this.id,
    name = this.name,
    icon = this.icon,
    color = this.color,
    keywords = this.keywords
)
// --- End of shared data classes ---


@HiltViewModel
class CategoryFormViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryMatcher: CategoryMatcher, // <-- MUST inject this
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryFormUiState())
    val uiState = _uiState.asStateFlow()

    private val categoryId: String? = savedStateHandle["categoryId"]

    init {
        if (categoryId != null) {
            // --- EDIT MODE ---
            _uiState.update { it.copy(isLoading = true, isEditMode = true) }
            loadCategoryForEdit(categoryId.toLong())
        } else {
            // --- ADD MODE ---
            _uiState.update {
                it.copy(isLoading = false, isEditMode = false)
            }
        }
    }

    private fun loadCategoryForEdit(id: Long) {
        viewModelScope.launch {
            // You will need to add `getCategoryById` to your repository
            val category = transactionRepository.getCategoryById(id) ?: return@launch
            _uiState.update {
                it.copy(isLoading = false, form = category.toFormData())
            }
        }
    }

    fun onFormStateChanged(newFormState: CategoryFormData) {
        _uiState.update { it.copy(form = newFormState) }
    }

    fun addKeywordToForm() {
        val keyword = _uiState.value.form.currentKeywordInput.trim()
        if (keyword.isNotBlank() && !uiState.value.form.keywords.contains(keyword)) {
            _uiState.update {
                val newKeywords = it.form.keywords + keyword
                it.copy(
                    form = it.form.copy(
                        keywords = newKeywords,
                        currentKeywordInput = ""
                    )
                )
            }
        }
    }

    fun removeKeywordFromForm(keyword: String) {
        _uiState.update {
            val newKeywords = it.form.keywords - keyword
            it.copy(form = it.form.copy(keywords = newKeywords))
        }
    }

    fun saveCategory() {
        val form = _uiState.value.form
        if (form.name.isBlank()) return // Simple validation

        viewModelScope.launch {
            val categoryToSave = Category(
                id = form.id ?: 0L, // 0L lets Room auto-generate
                name = form.name.trim(),
                icon = form.icon,
                color = form.color,
                keywords = form.keywords
            )

            if (_uiState.value.isEditMode) {
                transactionRepository.updateCategory(categoryToSave)
            } else {
                transactionRepository.insertCategory(categoryToSave)
            }

            // --- CRITICAL LINK ---
            // Invalidate the cache so the matcher sees the changes
            categoryMatcher.invalidateCache()
        }
    }
}