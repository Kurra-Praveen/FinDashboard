package com.kpr.fintrack.presentation.ui.shared

import androidx.compose.runtime.compositionLocalOf
import com.kpr.fintrack.domain.model.Category

/**
 * CompositionLocal for providing categories throughout the composition tree.
 * This eliminates the need to create ViewModel instances in child composables,
 * significantly reducing recompositions and improving performance.
 * 
 * Usage:
 * - Provide at screen level: CompositionLocalProvider(LocalCategories provides categories)
 * - Access in child: val categories = LocalCategories.current
 */
val LocalCategories = compositionLocalOf<List<Category>> {
    error("No categories provided. Make sure to provide categories at screen level using CompositionLocalProvider.")
}
