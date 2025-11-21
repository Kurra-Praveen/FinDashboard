package com.kpr.fintrack.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * FinTrack Enhanced Color System
 * Phase 2: UI Polish & Visual Enhancement
 * 
 * Modern, semantic color palette for consistent and beautiful UI
 */

// ============================================================================
// BRAND COLORS
// ============================================================================

/**
 * Primary brand colors - Used for main actions, emphasis, and branding
 */
object BrandColors {
    val Primary = Color(0xFF0066FF)           // Vibrant blue - main brand color
    val PrimaryVariant = Color(0xFF0052CC)    // Darker blue for depth
    val Secondary = Color(0xFF00C896)         // Mint green - secondary actions
    val SecondaryVariant = Color(0xFF00A676)  // Darker mint for contrast
}

// ============================================================================
// SEMANTIC COLORS
// ============================================================================

/**
 * Semantic colors for UI feedback and states
 * These communicate meaning and status to users
 */
object SemanticColors {
    // Success - Positive actions, completed states
    val Success = Color(0xFF2ED573)           // Vibrant green
    val SuccessContainer = Color(0xFFD4F4DD)  // Light green background
    val OnSuccess = Color(0xFF005313)         // Dark green text
    
    // Warning - Caution, important but not critical
    val Warning = Color(0xFFFFA502)           // Amber orange
    val WarningContainer = Color(0xFFFFEFCC)  // Light amber background
    val OnWarning = Color(0xFF5C3A00)         // Dark amber text
    
    // Error - Destructive actions, errors, critical issues
    val Error = Color(0xFFFF4757)             // Warm red
    val ErrorContainer = Color(0xFFFFDAD6)    // Light red background
    val OnError = Color(0xFF690005)           // Dark red text
    
    // Info - Informational messages, neutral states
    val Info = Color(0xFF5F27CD)              // Purple
    val InfoContainer = Color(0xFFE8DDFF)     // Light purple background
    val OnInfo = Color(0xFF2A0080)            // Dark purple text
}

// ============================================================================
// TRANSACTION COLORS
// ============================================================================

/**
 * Colors for different transaction types
 * Provides visual distinction for financial operations
 */
object TransactionColors {
    val Debit = Color(0xFFFF4757)             // Warm red - money going out
    val Credit = Color(0xFF2ED573)            // Success green - money coming in
    val Pending = Color(0xFFFFA502)           // Amber - awaiting confirmation
    val Transfer = Color(0xFF5F27CD)          // Purple - internal transfers
}

// ============================================================================
// SURFACE COLORS
// ============================================================================

/**
 * Surface and background colors for depth and hierarchy
 * Light theme surfaces
 */
object SurfaceColors {
    // Light theme
    val SurfaceLight = Color(0xFFFDFBFF)      // Primary surface
    val Surface1Light = Color(0xFFF5F5F7)     // Elevated surface level 1
    val Surface2Light = Color(0xFFEFEFF1)     // Elevated surface level 2
    val Surface3Light = Color(0xFFE8E8EA)     // Elevated surface level 3
    
    // Dark theme
    val SurfaceDark = Color(0xFF1A1C1E)       // Primary surface
    val Surface1Dark = Color(0xFF2B2D30)      // Elevated surface level 1
    val Surface2Dark = Color(0xFF363A3D)      // Elevated surface level 2
    val Surface3Dark = Color(0xFF424548)      // Elevated surface level 3
}

// ============================================================================
// GRADIENT UTILITIES
// ============================================================================

/**
 * Pre-defined gradients for modern, polished UI elements
 * Use these for cards, buttons, and decorative elements
 */
object FinTrackGradients {
    /**
     * Primary brand gradient - Vibrant blue flow
     */
    val PrimaryGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0066FF),  // Bright blue
            Color(0xFF0052CC)   // Deep blue
        )
    )
    
    /**
     * Success gradient - Positive vibes
     */
    val SuccessGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF2ED573),  // Vibrant green
            Color(0xFF00A676)   // Deep teal
        )
    )
    
    /**
     * Sunset gradient - Warm, inviting
     */
    val SunsetGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFF6B6B),  // Coral red
            Color(0xFFFFA502)   // Amber orange
        )
    )
    
    /**
     * Ocean gradient - Cool, calming
     */
    val OceanGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF4ECDC4),  // Turquoise
            Color(0xFF0066FF)   // Blue
        )
    )
    
    /**
     * Purple gradient - Premium, sophisticated
     */
    val PurpleGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF5F27CD),  // Purple
            Color(0xFF341F97)   // Deep purple
        )
    )
    
    /**
     * Subtle surface gradient for elevated cards
     */
    val SurfaceGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFDFBFF),
            Color(0xFFF5F5F7)
        )
    )
}

// ============================================================================
// CATEGORY COLORS (Enhanced)
// ============================================================================

/**
 * Vibrant colors for budget categories
 * Updated with modern, saturated colors for better visual distinction
 */
object CategoryColors {
    val Food = Color(0xFFFF6B6B)              // Coral red
    val Transport = Color(0xFF4ECDC4)         // Turquoise
    val Shopping = Color(0xFF45B7D1)          // Sky blue
    val Bills = Color(0xFF96CEB4)             // Sage green
    val Healthcare = Color(0xFFFECA57)        // Sunny yellow
    val Entertainment = Color(0xFFFF9FF3)     // Pink
    val Education = Color(0xFF5F27CD)         // Purple
    val Savings = Color(0xFF00C896)           // Mint green
    val Investment = Color(0xFF0066FF)        // Blue
    val Other = Color(0xFF95A5A6)             // Gray
}

// ============================================================================
// LEGACY COLORS (Deprecated - kept for backwards compatibility)
// ============================================================================

@Deprecated("Use BrandColors.Primary instead", ReplaceWith("BrandColors.Primary"))
val FinTrackPrimary = Color(0xFF1976D2)

@Deprecated("Use BrandColors.PrimaryVariant instead", ReplaceWith("BrandColors.PrimaryVariant"))
val FinTrackPrimaryVariant = Color(0xFF0D47A1)

@Deprecated("Use BrandColors.Secondary instead", ReplaceWith("BrandColors.Secondary"))
val FinTrackSecondary = Color(0xFF4CAF50)

@Deprecated("Use BrandColors.SecondaryVariant instead", ReplaceWith("BrandColors.SecondaryVariant"))
val FinTrackSecondaryVariant = Color(0xFF2E7D32)

@Deprecated("Use TransactionColors.Debit instead", ReplaceWith("TransactionColors.Debit"))
val DebitColor = Color(0xFFE57373)

@Deprecated("Use TransactionColors.Credit instead", ReplaceWith("TransactionColors.Credit"))
val CreditColor = Color(0xFF81C784)

@Deprecated("Use TransactionColors.Transfer instead", ReplaceWith("TransactionColors.Transfer"))
val TransferColor = Color(0xFF64B5F6)

// Standard Material colors (kept for compatibility)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
