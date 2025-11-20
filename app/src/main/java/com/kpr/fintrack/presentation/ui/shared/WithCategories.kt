package com.kpr.fintrack.presentation.ui.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.kpr.fintrack.domain.model.Category

/**
 * Utility composable that provides categories to its content via CompositionLocal.
 * 
 * Wrap any screen that uses CategoryIcon with this composable to avoid crashes.
 * 
 * Usage:
 * ```
 * WithCategories {
 *     YourScreenContent()
 * }
 * ```
 */
@Composable
fun WithCategories(
    content: @Composable () -> Unit
) {
    val categoriesViewModel: CategoriesViewModel = hiltViewModel()
    val categories by categoriesViewModel.categories.collectAsState()
    
    CompositionLocalProvider(LocalCategories provides categories) {
        content()
    }
}
