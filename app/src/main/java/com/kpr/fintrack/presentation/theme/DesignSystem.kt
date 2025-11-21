package com.kpr.fintrack.presentation.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * FinTrack Design System
 * Centralized design tokens for consistent design language across the app
 * Phase 2: UI Polish & Visual Enhancement
 */
object FinTrackDesignSystem {
    
    /**
     * Spacing scale based on 4px grid system
     * Use these instead of hardcoded values for consistent spacing
     */
    object Spacing {
        val none: Dp = 0.dp
        val xxs: Dp = 2.dp    // Micro spacing (icon padding)
        val xs: Dp = 4.dp     // Minimal spacing
        val sm: Dp = 8.dp     // Small spacing (compact lists)
        val md: Dp = 12.dp    // Medium spacing (between related elements)
        val lg: Dp = 16.dp    // Large spacing (card padding)
        val xl: Dp = 24.dp    // Extra large (section spacing)
        val xxl: Dp = 32.dp   // Section headers
        val xxxl: Dp = 48.dp  // Major sections
        val xxxxl: Dp = 64.dp // Screen padding
    }
    
    /**
     * Elevation levels for cards and surfaces
     * Material 3 uses tonal elevation with surface tints
     */
    object Elevation {
        val none: Dp = 0.dp
        val xs: Dp = 1.dp    // Subtle elevation
        val sm: Dp = 2.dp    // Default cards
        val md: Dp = 4.dp    // Elevated cards
        val lg: Dp = 8.dp    // Floating elements
        val xl: Dp = 12.dp   // Dialogs
        val xxl: Dp = 16.dp  // Modal sheets
    }
    
    /**
     * Corner radius values for consistent rounded corners
     */
    object CornerRadius {
        val xs: Dp = 4.dp    // Minimal rounding
        val sm: Dp = 8.dp    // Small components
        val md: Dp = 12.dp   // Default cards
        val lg: Dp = 16.dp   // Large cards
        val xl: Dp = 20.dp   // Prominent elements
        val xxl: Dp = 24.dp  // Hero cards
        val pill: Dp = 999.dp // Fully rounded (buttons, chips)
    }
    
    /**
     * Animation duration constants (in milliseconds)
     * Use these for consistent timing across animations
     */
    object AnimationDuration {
        const val instant = 0      // No animation
        const val fast = 150       // Quick transitions
        const val normal = 300     // Standard animations
        const val slow = 500       // Deliberate, emphasized animations
        const val verySlow = 800   // Hero animations
    }
    
    /**
     * Icon sizes for consistency
     */
    object IconSize {
        val xs: Dp = 16.dp   // Small icons in lists
        val sm: Dp = 20.dp   // Default
        val md: Dp = 24.dp   // Standard
        val lg: Dp = 32.dp   // Large icons
        val xl: Dp = 48.dp   // Hero icons
    }
}
