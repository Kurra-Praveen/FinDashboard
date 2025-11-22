package com.kpr.fintrack.presentation.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Alignment

/**
 * FinTrack Animation Utilities
 * Modern, spring-based animations for sleek and fluid UI interactions
 * Phase 2: UI Polish & Visual Enhancement
 */

// ============================================================================
// SPRING SPECIFICATIONS
// ============================================================================

/**
 * Pre-defined spring specs for different animation feels
 */
object FinTrackSprings {
    /**
     * Bouncy spring - playful, noticeable bounce
     * Perfect for: Success celebrations, emphasized actions
     */
    val Bouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    /**
     * Smooth spring - subtle bounce, professional feel
     * Perfect for: Card animations, list items, general UI
     */
    val SmoothOffset: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )


    /**
     * Snappy spring - quick, minimal bounce
     * Perfect for: Button presses, micro-interactions
     */
    val Snappy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )
    
    /**
     * Gentle spring - soft, slow animation
     * Perfect for: Large elements, screen transitions
     */
    val Gentle = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessVeryLow
    )
}

// ============================================================================
// ENTRANCE ANIMATIONS
// ============================================================================

/**
 * Slide-up entrance with spring physics
 */
fun slideUpEnter(
    initialOffsetY: Dp = 50.dp,
    spring: SpringSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
) = slideInVertically(
    animationSpec = spring,
    initialOffsetY = { initialOffsetY.value.toInt() }
) + fadeIn(animationSpec = tween(FinTrackDesignSystem.AnimationDuration.normal))


/**
 * Scale-in entrance with spring physics
 */
fun scaleInEnter(
    initialScale: Float = 0.8f,
    spring: SpringSpec<IntSize> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
) = fadeIn(animationSpec = tween(FinTrackDesignSystem.AnimationDuration.fast)) +
        expandIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            expandFrom = Alignment.CenterStart   // ← replacement for initialOffsetX
        ) +
        fadeIn(animationSpec = tween(FinTrackDesignSystem.AnimationDuration.normal))

// ============================================================================
// EXIT ANIMATIONS
// ============================================================================

/**
 * Slide-down exit
 */
fun slideDownExit() = slideOutVertically(
    animationSpec = tween(FinTrackDesignSystem.AnimationDuration.fast),
    targetOffsetY = { it / 2 }
) + fadeOut(animationSpec = tween(FinTrackDesignSystem.AnimationDuration.fast))

/**
 * Scale-out exit
 */
fun scaleOutExit() = fadeOut(animationSpec = tween(FinTrackDesignSystem.AnimationDuration.fast)) +
        shrinkOut(
            animationSpec = tween(FinTrackDesignSystem.AnimationDuration.fast),
            shrinkTowards = androidx.compose.ui.Alignment.Center
        )

// ============================================================================
// MODIFIER EXTENSIONS - MICRO-INTERACTIONS
// ============================================================================

/**
 * Adds a press animation that scales down slightly on press
 * Perfect for buttons and clickable cards
 * 
 * @param targetScale Scale to animate to when pressed (default 0.95 = 95% size)
 * @param onClick Callback when pressed
 */
@Composable
fun Modifier.pressAnimation(
    targetScale: Float = 0.95f,
    onClick: () -> Unit
): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = FinTrackSprings.Snappy,
        label = "pressScale"
    )
    
    return this
        .scale(scale)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = { onClick() }
            )
        }
}

/**
 * Adds a shimmer/shine effect for emphasis
 * Great for highlighting new items or success states
 */
@Composable
fun Modifier.shimmerEffect(
    durationMillis: Int = 1500,
    enabled: Boolean = true
): Modifier {
    if (!enabled) return this
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis),
            repeatMode = RepeatMode.Reverse
        ),
    )
    
    return this.graphicsLayer { this.alpha = alpha }
}

/**
 * Entrance animation for list items
 * Staggered slide-up effect
 */
@Composable
fun Modifier.listItemEntranceAnimation(
    index: Int,
    delayPerItem: Int = 30
): Modifier {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * delayPerItem).toLong())
        visible = true
    }
    
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 30.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "listItemOffset"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(FinTrackDesignSystem.AnimationDuration.normal),
        label = "listItemAlpha"
    )
    
    return this
        .graphicsLayer {
            translationY = offsetY.toPx()
            this.alpha = alpha
        }
}

/**
 * Card entrance animation that works without composable context
 * Use with AnimatedVisibility for best results
 */
@Composable
fun Modifier.cardEntranceAnimation(
    delay: Int = 0
): Modifier {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        if (delay > 0) kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }
    
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = FinTrackSprings.Bouncy,
        label = "cardScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(FinTrackDesignSystem.AnimationDuration.normal),
        label = "cardAlpha"
    )
    
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
}


/**
 * Pulsing animation for attention-grabbing elements
 */
@Composable
fun Modifier.pulseAnimation(
    targetScale: Float = 1.05f,
    durationMillis: Int = 1000
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = targetScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    return this.scale(scale)
}

// ============================================================================
// CONTENT ANIMATIONS
// ============================================================================

/**
 * Animated cross-fade between content states
 */
@Composable
fun <T> AnimatedContent(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    androidx.compose.animation.AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            fadeIn(animationSpec = tween(FinTrackDesignSystem.AnimationDuration.normal)) togetherWith
                    fadeOut(animationSpec = tween(FinTrackDesignSystem.AnimationDuration.fast))
        },
        label = "contentChange"
    ) { state ->
        content(state)
    }
}
