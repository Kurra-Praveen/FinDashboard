package com.kpr.fintrack.presentation.theme

import androidx.compose.animation.core.Spring

/**
 * Animation Constants for FinTrack
 * 
 * Central location for all animation timing values to ensure consistency
 * and easy adjustment across the application.
 * 
 * ## How Animation Physics Work:
 * 
 * ### Stiffness (Spring Tension)
 * - Controls how fast the animation moves
 * - Higher values = Faster animation
 * - Lower values = Slower, more dramatic animation
 * 
 * Example values:
 * - 30f  = Very slow, dramatic
 * - 50f  = Slow, visible
 * - 100f = Medium speed
 * - 200f = Fast
 * - 300f = Very fast
 * 
 * ### Damping Ratio (Bounce)
 * - Controls overshoot and bounce behavior
 * - 1.0f  = No bounce (critically damped) - smoothest
 * - 0.8f  = Minimal bounce
 * - 0.6f  = Slight bounce (feels natural)
 * - 0.5f  = Noticeable bounce
 * - < 0.5 = Very bouncy
 * 
 * ### Delay (milliseconds)
 * - Time to wait before starting animation
 * - Used for staggered entrance animations
 */
object AnimationConstants {
    
    // ========================================================================
    // BUDGET PROGRESS ANIMATIONS
    // ========================================================================
    
    /**
     * Budget Progress Bar Fill Animation
     * 
     * Current: Slow, visible fill from 0 to target percentage
     * - Stiffness: 50f  (slow for visibility)
     * - Damping: 0.6f   (slight natural bounce)
     * 
     * To adjust speed:
     * - Faster: 100f, 150f, 200f
     * - Slower: 30f, 40f
     */
    object BudgetProgress {
        const val FILL_STIFFNESS = 50f
        const val FILL_DAMPING_RATIO = 0.6f
        
        // Example alternative configurations (commented out)
        // const val FILL_STIFFNESS = 100f  // Medium speed
        // const val FILL_STIFFNESS = 30f   // Very slow, dramatic
        // const val FILL_DAMPING_RATIO = 0.8f  // Less bounce
    }
    
    // ========================================================================
    // CARD ENTRANCE ANIMATIONS
    // ========================================================================
    
    /**
     * Card Entrance Animation Delays
     * 
     * Used for staggered card appearances on screens like Dashboard and Budget.
     * Delays are in milliseconds.
     */
    object CardEntrance {
        const val INITIAL_DELAY_MS = 50      // First card delay
        const val STAGGER_DELAY_MS = 50      // Delay between subsequent cards
        
        // Spring physics for card movement
        const val STIFFNESS = Spring.StiffnessMedium  // 1500f
        const val DAMPING_RATIO = Spring.DampingRatioMediumBouncy  // 0.7f
    }
    
    // ========================================================================
    // LIST ITEM ENTRANCE ANIMATIONS
    // ========================================================================
    
    /**
     * List Item Entrance Animation
     * 
     * Used for items in LazyColumn/LazyRow (e.g., transaction items, budget categories)
     */
    object ListItemEntrance {
        const val BASE_DELAY_MS = 0          // Base delay before first item
        const val ITEM_DELAY_MS = 30         // Delay per item index
        
        // Spring physics
        const val STIFFNESS = Spring.StiffnessMedium
        const val DAMPING_RATIO = Spring.DampingRatioMediumBouncy
    }
    
    // ========================================================================
    // SCREEN TRANSITIONS
    // ========================================================================
    
    /**
     * Screen-to-Screen Transition Animations
     * 
     * Used in AnimatedContent for state changes
     */
    object ScreenTransition {
        const val FADE_STIFFNESS = 200f
        const val SLIDE_STIFFNESS = 300f
    }
    
    // ========================================================================
    // CONTENT ANIMATIONS
    // ========================================================================
    
    /**
     * Content Size Animation
     * 
     * Used when content expands/collapses (e.g., expanding cards, text changes)
     */
    object ContentSize {
        const val STIFFNESS = Spring.StiffnessMediumLow  // 400f
        const val DAMPING_RATIO = Spring.DampingRatioNoBouncy  // 1.0f
    }
    
    // ========================================================================
    // BUTTON & INTERACTION ANIMATIONS
    // ========================================================================
    
    /**
     * Button Press Animation
     * 
     * Quick, responsive feedback for user interactions
     */
    object ButtonPress {
        const val SCALE_STIFFNESS = 400f     // Fast response
        const val SCALE_DAMPING = 0.5f       // Slight bounce
        const val SCALE_DOWN = 0.95f         // Press down to 95%
        const val SCALE_NORMAL = 1.0f        // Normal size
    }
    
    // ========================================================================
    // COMMON SPRING PRESETS
    // ========================================================================
    
    /**
     * Pre-configured spring animation specs for common use cases
     */
    object SpringPresets {
        // Quick, snappy animations
        const val QUICK_STIFFNESS = 300f
        const val QUICK_DAMPING = 0.7f
        
        // Slow, smooth animations
        const val SLOW_STIFFNESS = 100f
        const val SLOW_DAMPING = 0.8f
        
        // Bouncy, playful animations
        const val BOUNCY_STIFFNESS = 200f
        const val BOUNCY_DAMPING = 0.5f
        
        // Smooth, professional (no bounce)
        const val SMOOTH_STIFFNESS = 250f
        const val SMOOTH_DAMPING = 1.0f
    }
}

/**
 * Quick Reference Guide:
 * 
 * STIFFNESS GUIDE:
 * ----------------
 * 30-50f   → Very slow, dramatic (progress fills, reveals)
 * 100-150f → Medium speed (general UI transitions)
 * 200-300f → Fast (screen transitions, quick feedback)
 * 400+f    → Very fast (button presses, instant feedback)
 * 
 * DAMPING RATIO GUIDE:
 * -------------------
 * 1.0f     → No bounce (smooth, professional)
 * 0.7-0.8f → Minimal bounce (Material Design standard)
 * 0.5-0.6f → Noticeable bounce (playful, engaging)
 * < 0.5f   → Very bouncy (attention-grabbing)
 * 
 * ANIMATION DURATION (approximate):
 * ---------------------------------
 * The actual duration depends on both stiffness and damping:
 * - High stiffness + high damping = Very fast, smooth
 * - Low stiffness + low damping = Slow with bounce
 * - Low stiffness + high damping = Slow, smooth
 * - High stiffness + low damping = Fast with bounce
 */
