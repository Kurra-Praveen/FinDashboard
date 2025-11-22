package com.kpr.fintrack.presentation.ui.transaction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpr.fintrack.presentation.ui.components.ErrorState
import androidx.hilt.navigation.compose.hiltViewModel
import com.kpr.fintrack.domain.model.Transaction
import com.kpr.fintrack.presentation.theme.CreditColor
import com.kpr.fintrack.presentation.theme.DebitColor
import com.kpr.fintrack.presentation.ui.components.CategorySelectionBottomSheet
import com.kpr.fintrack.presentation.ui.components.CategoryIcon
import com.kpr.fintrack.presentation.ui.shared.CategoriesViewModel
import com.kpr.fintrack.presentation.ui.shared.LocalCategories
import androidx.compose.runtime.CompositionLocalProvider
import com.kpr.fintrack.utils.FinTrackLogger
import com.kpr.fintrack.utils.image.ReceiptImageProcessor
import com.kpr.fintrack.utils.extensions.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.window.Dialog
import com.kpr.fintrack.presentation.theme.TransactionColors
import java.time.format.DateTimeFormatter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCategorySheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }

    val categoriesViewModel: CategoriesViewModel = hiltViewModel()
    val categories by categoriesViewModel.categories.collectAsState()

    CompositionLocalProvider(LocalCategories provides categories) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Transaction" else "Transaction Details",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Fix: Assign to local val first
                    val transaction = uiState.transaction
                    if (transaction != null) {
                        IconButton(
                            onClick = {
                                if (isEditing) {
                                    viewModel.saveChanges()
                                }
                                isEditing = !isEditing
                            }
                        ) {
                            Icon(
                                imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                                contentDescription = if (isEditing) "Save" else "Edit"
                            )
                        }

                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorState(
                        title = "Error loading transaction",
                        message = uiState.error.toString(),
                        onRetry = { viewModel.loadTransaction(transactionId) }
                    )
                }
            }

            else -> {
                // Fix: Assign to local val to enable smart cast
                val transaction = uiState.transaction
                if (transaction != null) {
                    TransactionDetailContent(
                        transaction = transaction,
                        isEditing = isEditing,
                        onCategoryClick = { showCategorySheet = true },
                        onNotesChange = viewModel::updateNotes,
                        onVerificationToggle = viewModel::toggleVerification,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
            }
        }

        // Category Selection Bottom Sheet
        // Fix: Use local val here too
        val transaction = uiState.transaction
        if (showCategorySheet && transaction != null) {
            CategorySelectionBottomSheet(
                currentCategory = transaction.category,
                onCategorySelected = { category ->
                    viewModel.updateCategory(category)
                    showCategorySheet = false
                },
                onDismiss = { showCategorySheet = false }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Transaction") },
                text = { Text("Are you sure you want to delete this transaction? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteTransaction()
                            showDeleteDialog = false
                            onNavigateBack()
                        }
                    ) {
                        Text(
                            text = "Delete",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
    }
}

@Composable
private fun TransactionDetailContent(
    transaction: Transaction,
    isEditing: Boolean,
    onCategoryClick: () -> Unit,
    onNotesChange: (String) -> Unit,
    onVerificationToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showReceiptDialog by remember { mutableStateOf(false) }
    var showMessageDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Amount Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${if (transaction.isDebit) "-" else "+"}${transaction.amount.formatCurrency()}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.isDebit) DebitColor else CreditColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (transaction.isDebit) "Expense" else "Income",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Basic Information
        TransactionInfoSection(
            title = "Transaction Details",
            items = listOf(
                "Merchant" to transaction.merchantName,
                "Date" to transaction.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a")),
                "Reference ID" to (transaction.referenceId ?: "Not available"),
                "Account" to (transaction.accountNumber?.let { "****$it" } ?: "Not available"),
                "UPI App" to (transaction.upiApp?.name ?: "Direct Bank")
            )
        )
        val hasImage = !transaction.receiptImagePath.isNullOrBlank()
        val hasSms = transaction.smsBody.isNotBlank()

        if (hasImage && hasSms) {
            // ✅ Both available
            ReceiptCard(onClick = { showReceiptDialog = true })
            SmsCard(transaction.smsBody, transaction.sender, transaction.date.toString())
        } else if (hasImage || hasSms) {
            // ⚠️ One missing
            if (hasImage) {
                ReceiptCard(onClick = { showReceiptDialog = true })
            } else {
                SmsCard(transaction.smsBody, transaction.sender, transaction.date.toString())
            }

        } else {
            // ❌ Neither available
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "No receipt or message available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }


        // Category Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = if (isEditing) onCategoryClick else { -> }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIcon(transaction.category.id)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = transaction.category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (isEditing) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Change category",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Notes Section
        var notes by remember { mutableStateOf(transaction.tags.joinToString(", ")) }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isEditing) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = {
                            notes = it
                            onNotesChange(it)
                        },
                        placeholder = { Text("Add notes...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                } else {
                    Text(
                        text = notes.ifBlank { "No notes added" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (notes.isNotBlank()) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Verification & Confidence
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Verification",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Manually Verified",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (transaction.isManuallyVerified) "Verified by you" else "Auto-detected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = transaction.isManuallyVerified,
                        onCheckedChange = { onVerificationToggle() },
                        enabled = isEditing
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Confidence Score",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${(transaction.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            transaction.confidence >= 0.9f -> TransactionColors.Credit
                            transaction.confidence >= 0.7f -> MaterialTheme.colorScheme.primary
                            else -> TransactionColors.Debit
                        }
                    )
                }

                LinearProgressIndicator(
                    progress = { transaction.confidence },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    color = when {
                        transaction.confidence >= 0.9f -> TransactionColors.Credit
                        transaction.confidence >= 0.7f -> MaterialTheme.colorScheme.primary
                        else -> TransactionColors.Debit
                    }
                )
            }
        }

        // Fullscreen receipt dialog
        if (showReceiptDialog && !transaction.receiptImagePath.isNullOrBlank()) {
            Dialog(onDismissRequest = { showReceiptDialog = false }) {
                val file = File(transaction.receiptImagePath)
                val bitmap = ReceiptImageProcessor.decodeFileToBitmap(file)
                if (bitmap != null) {
                    FinTrackLogger.d(
                        "FinTrack_Image",
                        "Loaded receipt for preview: ${file.absolutePath}"
                    )
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Receipt Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                        )
                        IconButton(
                            onClick = { showReceiptDialog = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                } else {
                    FinTrackLogger.w(
                        "FinTrack_Image",
                        "Failed to load receipt image for preview: ${file.absolutePath}"
                    )
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Unable to load receipt image")
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionInfoSection(
    title: String,
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
@Composable
private fun ReceiptCard(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Receipt",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Tap to view transaction receipt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(onClick = onClick) {
                Text("View Transaction")
            }
        }
    }
}

@Composable
private fun SmsCard(smsBody: String, sender: String?, date: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Original Message",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (smsBody.length > 200) smsBody.take(200) + "..." else smsBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) { append("From ") }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold)) { append(sender ?: "Unknown") }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) { append(" on ") }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold)) { append(date) }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
