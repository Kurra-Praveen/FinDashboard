package com.kpr.fintrack.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.kpr.fintrack.ui.theme.Typography

// --- ENHANCED COLOR SCHEME (LIGHT) ---
// Using Phase 2 enhanced color system with modern, vibrant colors
private val LightColorScheme = lightColorScheme(
    // Primary - Vibrant blue for main actions
    primary = BrandColors.Primary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4E3FF),
    onPrimaryContainer = Color(0xFF001D36),
    
    // Secondary - Mint green for secondary actions
    secondary = BrandColors.Secondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0F0E8),
    onSecondaryContainer = Color(0xFF00281C),
    
    // Tertiary - Purple for accents
    tertiary = SemanticColors.Info,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = SemanticColors.InfoContainer,
    onTertiaryContainer = SemanticColors.OnInfo,
    
    // Error - Warm red for destructive actions
    error = SemanticColors.Error,
    onError = Color(0xFFFFFFFF),
    errorContainer = SemanticColors.ErrorContainer,
    onErrorContainer = SemanticColors.OnError,
    
    // Background & Surfaces - Clean, modern hierarchy
    background = Color(0xFFFAFBFC),
    onBackground = Color(0xFF1A1C1E),
    surface = SurfaceColors.SurfaceLight,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = SurfaceColors.Surface1Light,
    onSurfaceVariant = Color(0xFF42474E),
    outline = Color(0xFF73777F),
)

// --- ENHANCED COLOR SCHEME (DARK) ---
// Dark theme optimized for OLED and nighttime viewing
private val DarkColorScheme = darkColorScheme(
    // Primary - Lighter blue for dark backgrounds
    primary = Color(0xFF5C9EFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD4E3FF),
    
    // Secondary - Softer mint for dark mode
    secondary = Color(0xFF7FE8C3),
    onSecondary = Color(0xFF00381E),
    secondaryContainer = Color(0xFF005330),
    onSecondaryContainer = Color(0xFFD0F0E8),
    
    // Tertiary - Lighter purple for visibility
    tertiary = Color(0xFFB794F6),
    onTertiary = Color(0xFF2A0080),
    tertiaryContainer = Color(0xFF3D1199),
    onTertiaryContainer = SemanticColors.InfoContainer,
    
    // Error - Softer red for dark mode
    error = Color(0xFFFF6B7A),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = SemanticColors.ErrorContainer,
    
    // Background & Surfaces - Deep, elevated hierarchy
    background = Color(0xFF121212),
    onBackground = Color(0xFFE3E2E6),
    surface = SurfaceColors.SurfaceDark,
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = SurfaceColors.Surface1Dark,
    onSurfaceVariant = Color(0xFFC3C6CF),
    outline = Color(0xFF8D9199),
)

@Composable
fun FinTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // DISABLED dynamic colors to showcase our vibrant custom color scheme!
    // Material You pulls colors from wallpaper - set to true to enable that
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Set system bar colors to TRANSPARENT
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()

            // Get the insets controller
            val insetsController = WindowCompat.getInsetsController(window, view)

            // Set the appearance of the system bar icons (light or dark)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}