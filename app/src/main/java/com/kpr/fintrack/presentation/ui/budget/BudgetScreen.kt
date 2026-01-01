package com.kpr.fintrack.presentation.ui.budget

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kpr.fintrack.domain.model.BudgetDetails
import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.presentation.ui.components.CategoryIcon
import com.kpr.fintrack.presentation.ui.components.EmptyStateMessage
import com.kpr.fintrack.presentation.ui.shared.WithCategories
import com.kpr.fintrack.utils.FormatUtils
import java.math.BigDecimal
import com.kpr.fintrack.presentation.theme.cardEntranceAnimation
import com.kpr.fintrack.presentation.theme.listItemEntranceAnimation
import com.kpr.fintrack.presentation.theme.AnimationConstants

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
    WithCategories {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Monthly Budgets") }, navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                })
            }) { padding ->
            AnimatedContent(
                targetState = uiState,
                modifier = Modifier.padding(padding),
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = AnimationConstants.ScreenTransition.FADE_STIFFNESS)) with
                            fadeOut(animationSpec = spring(stiffness = AnimationConstants.ScreenTransition.FADE_STIFFNESS))
                }
            ) { state ->
                when (state) {
                    is BudgetUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
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
                                        details = state.totalBudgetDetails,
                                        modifier = Modifier.cardEntranceAnimation(delay = AnimationConstants.CardEntrance.INITIAL_DELAY_MS)
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
                                itemsIndexed(
                                    items = state.categoryBudgetItems,
                                    key = { _, item -> item.category.id }
                                ) { index, item ->
                                    CategoryBudgetCard(
                                        item = item,
                                        onClick = {
                                            viewModel.showEditDialog(
                                                item.category, item.budgetDetails
                                            )
                                        },
                                        modifier = Modifier.listItemEntranceAnimation(index)
                                    )
                                }
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
    details: BudgetDetails?,
    modifier: Modifier = Modifier
) {
    val spent = details?.spent ?: BigDecimal.ZERO
    val total = details?.budget?.amount ?: BigDecimal.ZERO
    val progress = details?.progress ?: 0f

    Card(
        modifier = modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)
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
    item: CategoryBudgetUiItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = item.budgetDetails?.progress ?: 0f

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
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
    progress: Float,
    modifier: Modifier = Modifier
) {
    // State to track the animated progress, starting from 0 for fill animation
    var targetProgress by remember { mutableStateOf(0f) }
    
    // Trigger fill animation on first composition
    LaunchedEffect(progress) {
        targetProgress = progress
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(
            dampingRatio = AnimationConstants.BudgetProgress.FILL_DAMPING_RATIO,
            stiffness = AnimationConstants.BudgetProgress.FILL_STIFFNESS
        ),
        label = "ProgressFillAnimation"
    )

    // Calculate color based on actual progress (not animated)
    val progressColor = when {
        progress >= 1.0f -> MaterialTheme.colorScheme.error // Overspent (red)
        progress >= 0.9f -> Color(0xFFFF6B00) // Warning (orange)
        progress >= 0.7f -> Color(0xFFFFB800) // Caution (amber)
        else -> MaterialTheme.colorScheme.primary // Good (blue/green)
    }

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = progressColor,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        drawStopIndicator = {}
    )
}