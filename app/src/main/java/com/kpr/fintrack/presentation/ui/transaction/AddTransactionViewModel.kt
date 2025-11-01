package com.kpr.fintrack.presentation.ui.transaction

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.collectAsState
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kpr.fintrack.domain.model.Account
import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.model.QuickTransactionTemplate
import com.kpr.fintrack.domain.model.TransactionFormData
import com.kpr.fintrack.domain.repository.AccountRepository
import com.kpr.fintrack.domain.repository.TransactionRepository
import com.kpr.fintrack.presentation.ui.shared.CategoriesViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import javax.inject.Inject
import com.kpr.fintrack.utils.image.ReceiptImageProcessor
import com.kpr.fintrack.utils.FinTrackLogger
import com.kpr.fintrack.utils.parsing.CategoryMatcher
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class AddTransactionUiState(
    val formData: TransactionFormData = TransactionFormData(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val amountError: String? = null,
    val merchantError: String? = null,
    val isFormValid: Boolean = false,
    val capturedImageUri: Uri? = null
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryMatcher: CategoryMatcher,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()
    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    init {
        loadAccounts()
        loadCategories()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.getAllActiveAccounts().collect { accountsList ->
                _accounts.value = accountsList
            }
        }
    }

    private fun loadCategories() {
        categoryMatcher.getLiveCategoriesFlow()
            .onEach { list -> _categories.value = list }
            .launchIn(viewModelScope)
    }

    fun loadTransaction(transactionId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val transaction = transactionRepository.getTransactionById(transactionId)
                if (transaction != null) {
                    val formData = TransactionFormData.fromTransaction(transaction)
                    _uiState.value = _uiState.value.copy(
                        formData = formData,
                        isLoading = false
                    )
                    validateForm()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Transaction not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load transaction"
                )
            }
        }
    }

    fun applyTemplate(template: QuickTransactionTemplate) {
        val currentFormData = _uiState.value.formData
        val newFormData = currentFormData.copy(
            merchantName = template.merchantName,
            category = template.category,
            isDebit = template.isDebit,
            amount = template.suggestedAmount?.toPlainString() ?: currentFormData.amount
        )

        _uiState.value = _uiState.value.copy(formData = newFormData)
        validateForm()
    }

    fun onAmountChanged(amount: String) {
        val filteredAmount = amount.filter { it.isDigit() || it == '.' }
        val newFormData = _uiState.value.formData.copy(amount = filteredAmount)
        _uiState.value = _uiState.value.copy(formData = newFormData)
        validateForm()
    }

    fun onMerchantNameChanged(merchantName: String) {
        val newFormData = _uiState.value.formData.copy(merchantName = merchantName)
        _uiState.value = _uiState.value.copy(formData = newFormData)

        // Auto-suggest category based on merchant name
        suggestCategoryForMerchant(merchantName)
        validateForm()
    }

    fun onDescriptionChanged(description: String) {
        val newFormData = _uiState.value.formData.copy(description = description)
        _uiState.value = _uiState.value.copy(formData = newFormData)
    }

    fun onAccountChanged(account: Account?) {
        val newFormData = _uiState.value.formData.copy(account = account)
        _uiState.value = _uiState.value.copy(formData = newFormData)
    }

    fun onCategoryChanged(category: Category) {
        val newFormData = _uiState.value.formData.copy(category = category)
        _uiState.value = _uiState.value.copy(formData = newFormData)
        validateForm()
    }

    fun onDateChanged(date: LocalDateTime) {
        val newFormData = _uiState.value.formData.copy(date = date)
        _uiState.value = _uiState.value.copy(formData = newFormData)
    }

    fun onTransactionTypeChanged(isDebit: Boolean) {
        val newFormData = _uiState.value.formData.copy(isDebit = isDebit)
        _uiState.value = _uiState.value.copy(formData = newFormData)
        validateForm()
    }

    fun createImageUri(context: Context): Uri {
        val imageFile = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        _uiState.value = _uiState.value.copy(capturedImageUri = uri)
        return uri
    }

    fun onReceiptCaptured() {
        _uiState.value.capturedImageUri?.let { uri ->
            val newFormData = _uiState.value.formData.copy(receiptImagePath = uri.toString())
            _uiState.value = _uiState.value.copy(formData = newFormData)
        }
    }

    fun onReceiptRemoved() {
        val newFormData = _uiState.value.formData.copy(receiptImagePath = null)
        _uiState.value = _uiState.value.copy(formData = newFormData)
    }

    fun saveTransaction() {
        if (!_uiState.value.isFormValid) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)

                // Compress and persist receipt image if present
                val currentForm = _uiState.value.formData
                var finalReceiptPath: String? = currentForm.receiptImagePath

                if (!currentForm.receiptImagePath.isNullOrBlank()) {
                    try {
                        val pathStr = currentForm.receiptImagePath

                        // pathStr is non-null because of the outer check
                        if (pathStr!!.startsWith("content://") || pathStr.startsWith("file://")) {
                            val uri = Uri.parse(pathStr)
                            val receiptDir = File(context.cacheDir, "receipts").apply { mkdirs() }
                            val tempFile = File(receiptDir, "capture_${System.currentTimeMillis()}.tmp")

                            context.contentResolver.openInputStream(uri)?.use { input ->
                                FileOutputStream(tempFile).use { out ->
                                    input.copyTo(out)
                                }
                            }

                            val compressed = ReceiptImageProcessor.compressAndSaveToAppStorage(context, tempFile)
                            kotlin.runCatching { tempFile.delete() }
                            finalReceiptPath = compressed.absolutePath
                            FinTrackLogger.d("FinTrack_Image", "Compressed receipt saved: $finalReceiptPath")
                        } else {
                            // Treat as local file path
                            val f = File(pathStr)
                            if (f.exists()) {
                                val compressed = ReceiptImageProcessor.compressAndSaveToAppStorage(context, f)
                                finalReceiptPath = compressed.absolutePath
                                FinTrackLogger.d("FinTrack_Image", "Compressed receipt saved from file: $finalReceiptPath")
                            }
                        }
                    } catch (e: Exception) {
                        FinTrackLogger.e("FinTrack_Image", "Failed to compress receipt image", e)
                        finalReceiptPath = null
                    }
                }

                val newFormData = currentForm.copy(receiptImagePath = finalReceiptPath)
                val transaction = newFormData.toTransaction()

                if (transaction.id == 0L) {
                    // New transaction
                    transactionRepository.insertTransaction(transaction)
                } else {
                    // Update existing transaction
                    transactionRepository.updateTransaction(transaction)
                }

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Failed to save transaction"
                )
                FinTrackLogger.e("FinTrack_Image", "Error saving transaction", e)
            }
        }
    }

    private fun validateForm() {
        val formData = _uiState.value.formData

        // Validate amount
        val amountError = when {
            formData.amount.isBlank() -> "Amount is required"
            formData.amount.toBigDecimalOrNull() == null -> "Invalid amount"
            formData.amount.toBigDecimal() <= java.math.BigDecimal.ZERO -> "Amount must be greater than 0"
            else -> null
        }

        // Validate merchant name
        val merchantError = when {
            formData.merchantName.isBlank() -> "Merchant name is required"
            formData.merchantName.length < 2 -> "Merchant name too short"
            else -> null
        }

        val isFormValid = amountError == null && merchantError == null

        _uiState.value = _uiState.value.copy(
            amountError = amountError,
            merchantError = merchantError,
            isFormValid = isFormValid
        )
    }

    private fun suggestCategoryForMerchant(merchantName: String) {
        if (merchantName.length < 3) return

        val lowerName = merchantName.lowercase()
        val suggestedCategory = categories.value.find { category ->
            category.keywords.any { keyword ->
                lowerName.contains(keyword.lowercase()) || keyword.lowercase().contains(lowerName)
            }
        }

        if (suggestedCategory != null && suggestedCategory != _uiState.value.formData.category) {
            val newFormData = _uiState.value.formData.copy(category = suggestedCategory)
            _uiState.value = _uiState.value.copy(formData = newFormData)
        }
    }
}
