package com.kpr.fintrack.presentation.ui.budget

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kpr.fintrack.domain.model.BudgetDetails
import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.presentation.ui.components.EmptyStateMessage
import com.kpr.fintrack.presentation.ui.dashboard.CategoryIcon
import com.kpr.fintrack.utils.FormatUtils // Assuming you have this for formatting currency
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel(), onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()

    // --- Dialog ---
    dialogState?.let { state ->
        EditBudgetDialog(
            state = state,
            onDismiss = viewModel::dismissDialog,
            onSave = viewModel::onSaveBudget,
            onDelete = viewModel::onDeleteBudget
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Monthly Budgets") }, navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            })
        }) { padding ->
        AnimatedContent(
            targetState = uiState, modifier = Modifier.padding(padding), transitionSpec = {
                fadeIn(animationSpec = spring(stiffness = 200f)) with fadeOut(
                    animationSpec = spring(
                        stiffness = 200f
                    )
                )
            }) { state ->
            when (state) {
                is BudgetUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is BudgetUiState.Success -> {
                    if (state.categoryBudgetItems.isEmpty()) {
                        EmptyStateMessage(message = "No categories found. Please add a category first.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 1. Total Budget Card
                            item {
                                TotalBudgetCard(
                                    details = state.totalBudgetDetails
                                )
                            }

                            // 2. Header for Categories
                            item {
                                Text(
                                    text = "Category Budgets",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                )
                            }

                            // 3. Category Budgets List
                            items(state.categoryBudgetItems, key = { it.category.id }) { item ->
                                CategoryBudgetCard(
                                    item = item, onClick = {
                                        viewModel.showEditDialog(
                                            item.category, item.budgetDetails
                                        )
                                    })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TotalBudgetCard(
    details: BudgetDetails?
) {
    val spent = details?.spent ?: BigDecimal.ZERO
    val total = details?.budget?.amount ?: BigDecimal.ZERO
    val progress = details?.progress ?: 0f

    Card(
        modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize() // Smooth animation when text changes
        ) {
            Text(
                text = "Total Monthly Budget",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(12.dp))

            if (details != null) {
                // Formatting currency
                val spentFormatted = remember(spent) { FormatUtils.formatCurrency(spent) }
                val totalFormatted = remember(total) { FormatUtils.formatCurrency(total) }

                Text(
                    text = "$spentFormatted / $totalFormatted",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (details.isOverspent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                AnimatedProgressIndicator(
                    progress = progress, modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "Not set. Tap to add a total budget.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CategoryBudgetCard(
    item: CategoryBudgetUiItem, onClick: () -> Unit
) {
    val progress = item.budgetDetails?.progress ?: 0f

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            // You can use your existing icon component here
            // Icon(painter = painterResource(id = getIconRes(item.category.icon)), ... )
            CategoryIcon(item.category.id)

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(4.dp))

                if (item.budgetDetails != null) {
                    val spent = FormatUtils.formatCurrency(item.budgetDetails.spent)
                    val total = FormatUtils.formatCurrency(item.budgetDetails.budget.amount)

                    Text(
                        text = "$spent / $total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (item.budgetDetails.isOverspent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    AnimatedProgressIndicator(
                        progress = progress, modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "No budget set",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedProgressIndicator(
    progress: Float, modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = 250f),
        label = "ProgressAnimation"
    )

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = if (progress > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    )
}