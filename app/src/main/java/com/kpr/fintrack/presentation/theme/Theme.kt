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

// --- GENERATED PALETTE (LIGHT) ---
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2), // Your FinTrackPrimary
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4E3FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF4CAF50), // Your FinTrackSecondary
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0F0C8),
    onSecondaryContainer = Color(0xFF0D200B),
    tertiary = Color(0xFF7D5260), // Your Pink40
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFE57373), // Your DebitColor
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB), // For Cards
    onSurfaceVariant = Color(0xFF42474E), // For secondary text
    outline = Color(0xFF73777F),
)

// --- GENERATED PALETTE (DARK) ---
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA5C8FF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD4E3FF),
    secondary = Color(0xFFB4D4AE),
    onSecondary = Color(0xFF21361E),
    secondaryContainer = Color(0xFF374D33),
    onSecondaryContainer = Color(0xFFD0F0C8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFFC0392B),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF42474E), // For Cards
    onSurfaceVariant = Color(0xFFC3C6CF), // For secondary text
    outline = Color(0xFF8D9199),
)

@Composable
fun FinTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
        // --- THIS IS THE CORRECTED SIDEEFFECT ---
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

    // The Box element is gone, as it's no longer needed.

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}