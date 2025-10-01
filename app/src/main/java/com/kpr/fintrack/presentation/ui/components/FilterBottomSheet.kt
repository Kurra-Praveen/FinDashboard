package com.kpr.fintrack.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.repository.TransactionFilter
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.platform.LocalContext
import java.time.format.DateTimeFormatter
import android.app.DatePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentFilter: TransactionFilter,
    onFilterUpdate: (TransactionFilter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
        android.util.Log.d("FilterBottomSheet", "Composable entered")
    var selectedCategories by remember {
        mutableStateOf(currentFilter.categoryIds?.toSet() ?: emptySet())
    }
    var selectedTransactionType by remember {
        mutableStateOf(currentFilter.isDebit)
    }
    var minAmount by remember {
        mutableStateOf(currentFilter.minAmount?.toString() ?: "")
    }
    var maxAmount by remember {
        mutableStateOf(currentFilter.maxAmount?.toString() ?: "")
    }
    var startDate by remember {
        mutableStateOf(currentFilter.startDate)
    }
    var endDate by remember {
        mutableStateOf(currentFilter.endDate)
    }

    val context = LocalContext.current
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        modifier = modifier
    ) {
        LazyColumn (
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Filter Transactions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                TransactionTypeFilter(
                    selectedType = selectedTransactionType,
                    onTypeSelected = { selectedTransactionType = it }
                )
            }

            item {
                CategoryFilter(
                    selectedCategories = selectedCategories,
                    onCategoryToggle = { category ->
                        selectedCategories = if (selectedCategories.contains(category.id)) {
                            selectedCategories - category.id
                        } else {
                            selectedCategories + category.id
                        }
                    }
                )
            }

            item {
                AmountRangeFilter(
                    minAmount = minAmount,
                    maxAmount = maxAmount,
                    onMinAmountChange = { minAmount = it },
                    onMaxAmountChange = { maxAmount = it }
                )
            }

            item {
                DateRangeFilter(
                    startDate = startDate,
                    endDate = endDate,
                    onStartDateSelected = { startDate = it },
                    onEndDateSelected = { endDate = it },
                    onClearDates = {
                        startDate = null
                        endDate = null
                    },
                    dateFormatter = dateFormatter,
                    context = context
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onFilterUpdate(
                                currentFilter.copy(
                                    categoryIds = selectedCategories.takeIf { it.isNotEmpty() }?.toList(),
                                    isDebit = selectedTransactionType,
                                    minAmount = minAmount.takeIf { it.isNotBlank() }?.toBigDecimalOrNull(),
                                    maxAmount = maxAmount.takeIf { it.isNotBlank() }?.toBigDecimalOrNull(),
                                    startDate = startDate,
                                    endDate = endDate?.with (LocalTime.MAX) // Set end date to end of day
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply Filters")
                    }
                }
            }

            // Add bottom padding for the last item
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DateRangeFilter(
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    onStartDateSelected: (LocalDateTime) -> Unit,
    onEndDateSelected: (LocalDateTime) -> Unit,
    onClearDates: () -> Unit,
    dateFormatter: DateTimeFormatter,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Date Range",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (startDate != null || endDate != null) {
                IconButton(onClick = onClearDates) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear dates"
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val currentDate = startDate?.toLocalDate() ?: LocalDate.now()
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            onStartDateSelected(
                                LocalDateTime.of(
                                    LocalDate.of(year, month + 1, day),
                                    LocalTime.MIN
                                )
                            )
                        },
                        currentDate.year,
                        currentDate.monthValue - 1,
                        currentDate.dayOfMonth
                    ).show()
                },
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = startDate?.format(dateFormatter) ?: "Start Date",
                        maxLines = 1
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    val currentDate = endDate?.toLocalDate() ?: LocalDate.now()
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            onEndDateSelected(
                                LocalDateTime.of(
                                    LocalDate.of(year, month + 1, day),
                                    LocalTime.MIN
                                )
                            )
                        },
                        currentDate.year,
                        currentDate.monthValue - 1,
                        currentDate.dayOfMonth
                    ).show()
                },
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = endDate?.format(dateFormatter) ?: "End Date",
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionTypeFilter(
    selectedType: Boolean?,
    onTypeSelected: (Boolean?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Transaction Type",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedType == null,
                        onClick = { onTypeSelected(null) }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedType == null,
                    onClick = { onTypeSelected(null) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("All")
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedType == true,
                        onClick = { onTypeSelected(true) }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedType == true,
                    onClick = { onTypeSelected(true) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Expenses")
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedType == false,
                        onClick = { onTypeSelected(false) }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedType == false,
                    onClick = { onTypeSelected(false) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Income")
            }
        }
    }
}

@Composable
private fun CategoryFilter(
    selectedCategories: Set<Long>,
    onCategoryToggle: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Category.getDefaultCategories().forEach { category ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selectedCategories.contains(category.id),
                    onCheckedChange = { onCategoryToggle(category) }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(text = category.icon)

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = category.name,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AmountRangeFilter(
    minAmount: String,
    maxAmount: String,
    onMinAmountChange: (String) -> Unit,
    onMaxAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Amount Range",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = minAmount,
                onValueChange = onMinAmountChange,
                label = { Text("Min Amount") },
                placeholder = { Text("0") },
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = maxAmount,
                onValueChange = onMaxAmountChange,
                label = { Text("Max Amount") },
                placeholder = { Text("No limit") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
