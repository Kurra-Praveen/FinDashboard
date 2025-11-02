package com.kpr.fintrack.presentation.ui.transaction

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.model.QuickTransactionTemplate
import com.kpr.fintrack.presentation.ui.components.CategorySelectionBottomSheet
import com.kpr.fintrack.presentation.ui.components.DateTimePickerDialog
import java.time.format.DateTimeFormatter
import androidx.core.graphics.toColorInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    transactionId: Long? = null, // null for new, id for edit
    onNavigateBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val context = LocalContext.current

    var showCategorySheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Camera launcher for receipt photos
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.onReceiptCaptured()
        }
    }

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            viewModel.loadTransaction(transactionId)
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (transactionId == null) "Add Transaction" else "Edit Transaction",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveTransaction() },
                        enabled = uiState.isFormValid && !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {

            // Quick Templates (only for new transactions)
            if (transactionId == null) {
                QuickTemplatesSection(
                    onTemplateClick = { template ->
                        viewModel.applyTemplate(template)
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            // Transaction Form
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Transaction Type Selector
                TransactionTypeSelector(
                    isDebit = uiState.formData.isDebit,
                    onTypeChanged = viewModel::onTransactionTypeChanged
                )

                // Amount Input
                OutlinedTextField(
                    value = uiState.formData.amount,
                    onValueChange = viewModel::onAmountChanged,
                    label = { Text("Amount") },
                    placeholder = { Text("0.00") },
                    leadingIcon = { Text("₹", style = MaterialTheme.typography.titleMedium) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.amountError != null,
                    supportingText = uiState.amountError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Merchant Name
                OutlinedTextField(
                    value = uiState.formData.merchantName,
                    onValueChange = viewModel::onMerchantNameChanged,
                    label = { Text(if (uiState.formData.isDebit) "Paid To" else "Received From") },
                    placeholder = { Text("Enter merchant name") },
                    leadingIcon = {
                        Icon(
                            if (uiState.formData.isDebit) Icons.Default.Store else Icons.Default.Person,
                            contentDescription = null
                        )
                    },
                    isError = uiState.merchantError != null,
                    supportingText = uiState.merchantError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selection
                OutlinedCard(
                    onClick = { showCategorySheet = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.formData.category.icon,
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Category",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.formData.category.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Select category",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Account Selection
                OutlinedCard(
                    onClick = { showAccountSheet = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Account",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.formData.account?.name ?: "Select Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (uiState.formData.account == null)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Select account",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Date Selection
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Date & Time",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.formData.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a")),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit date",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Description
                OutlinedTextField(
                    value = uiState.formData.description,
                    onValueChange = viewModel::onDescriptionChanged,
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Add a note...") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Receipt Photo Section
                ReceiptPhotoSection(
                    imagePath = uiState.formData.receiptImagePath,
                    onCaptureClick = {
                        val imageUri = viewModel.createImageUri(context)
                        cameraLauncher.launch(imageUri)
                    },
                    onRemoveClick = viewModel::onReceiptRemoved
                )

                // Save Button (bottom space)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Category Selection Bottom Sheet
        if (showCategorySheet) {
            CategorySelectionBottomSheet(
                currentCategory = uiState.formData.category,
                onCategorySelected = { category ->
                    viewModel.onCategoryChanged(category)
                    showCategorySheet = false
                },
                onDismiss = { showCategorySheet = false }
            )
        }

        // Account Selection Bottom Sheet
        if (showAccountSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAccountSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Select Account",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Account list
                    accounts.forEach { account ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = account.name,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            supportingContent = account.currentBalance?.let { balance ->
                                { Text("Balance: ₹${balance}") }
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = account.color?.let {
                                        Color(it.toColorInt())
                                    } ?: MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = if (uiState.formData.account?.id == account.id) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else null,
                            modifier = Modifier.clickable {
                                viewModel.onAccountChanged(account)
                                showAccountSheet = false
                            }
                        )
                    }

                    // Clear selection option
                    if (uiState.formData.account != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ListItem(
                            headlineContent = { Text("Clear Selection") },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.onAccountChanged(null)
                                showAccountSheet = false
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Date Picker Dialog
        if (showDatePicker) {
            DateTimePickerDialog(
                initialDateTime = uiState.formData.date,
                onDateTimeSelected = { dateTime ->
                    viewModel.onDateChanged(dateTime)
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }

        // Error Snackbar
        uiState.errorMessage?.let { error ->
            LaunchedEffect(error) {
                // Show snackbar - you can implement SnackbarHost if needed
                android.util.Log.e("AddTransaction", error)
            }
        }
    }
}

@Composable
private fun QuickTemplatesSection(
    onTemplateClick: (QuickTransactionTemplate) -> Unit
) {
    // ✅ Get templates safely
    val templates = remember { QuickTransactionTemplate.getDefaultTemplates() }

    // ✅ Only show section if templates exist
    if (templates.isNotEmpty()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick Add",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(templates) { template ->
                    QuickTemplateCard(
                        template = template,
                        onClick = { onTemplateClick(template) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickTemplateCard(
    template: QuickTransactionTemplate,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(120.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = template.icon,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = template.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )

            template.suggestedAmount?.let { amount ->
                Text(
                    text = "₹${amount.toPlainString()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TransactionTypeSelector(
    isDebit: Boolean,
    onTypeChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            onClick = { onTypeChanged(true) },
            label = { Text("Expense") },
            selected = isDebit,
            leadingIcon = if (isDebit) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else null,
            modifier = Modifier.weight(1f)
        )

        FilterChip(
            onClick = { onTypeChanged(false) },
            label = { Text("Income") },
            selected = !isDebit,
            leadingIcon = if (!isDebit) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else null,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ReceiptPhotoSection(
    imagePath: String?,
    onCaptureClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Receipt Photo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            if (imagePath != null) {
                TextButton(onClick = onRemoveClick) {
                    Text("Remove")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (imagePath != null) {
            // Show captured receipt (you can implement image display here)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Photo,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Receipt captured",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        } else {
            OutlinedCard(
                onClick = onCaptureClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("Capture Receipt (Optional)")
                }
            }
        }
    }
}