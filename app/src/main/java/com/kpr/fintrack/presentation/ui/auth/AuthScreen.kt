package com.kpr.fintrack.presentation.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.kpr.fintrack.presentation.navigation.Screen

// Helper function to find the AppCompatActivity from the Composable context.
// This is required by the BiometricPrompt.
private fun Context.findActivity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun AuthScreen(
    navController: NavHostController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authStatus by viewModel.authStatus.collectAsState()
    val context = LocalView.current.context

    LaunchedEffect(authStatus) {
        when (authStatus) {
            AuthStatus.NO_AUTH_REQUIRED, AuthStatus.AUTH_SUCCESS -> {
                viewModel.logAuthScreenEvent("Auth successful or not required. Navigating to Dashboard.")
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            }
            AuthStatus.BIOMETRIC_REQUIRED -> {
                viewModel.logAuthScreenEvent("State is BIOMETRIC_REQUIRED. Finding activity...")

                // --- START NEW DIAGNOSTIC LOGS ---
                // Let's manually traverse the context chain to see what we have
                var currentContext: Context? = context
                var depth = 0
                viewModel.logAuthScreenEvent("--- Context Chain Inspection ---")
                while (currentContext != null && depth < 10) { // Limit to 10 levels
                    viewModel.logAuthScreenEvent("Context chain [$depth]: ${currentContext.javaClass.name}")
                    if (currentContext is AppCompatActivity) {
                        viewModel.logAuthScreenEvent("Context chain [$depth]: FOUND AppCompatActivity!")
                        break
                    }
                    if (currentContext is Activity) {
                        viewModel.logAuthScreenEvent("Context chain [$depth]: FOUND Activity (but not AppCompat).")
                    }

                    currentContext = (currentContext as? ContextWrapper)?.baseContext
                    depth++
                }
                viewModel.logAuthScreenEvent("--- End Inspection ---")
                // --- END NEW DIAGNOSTIC LOGS ---


                val activity = context.findActivity()
                if (activity != null) {
                    viewModel.logAuthScreenEvent("Activity found. Triggering prompt.")
                    viewModel.triggerBiometricPrompt(activity)
                } else {
                    viewModel.logAuthScreenEvent("ERROR: Activity was null via findActivity(). Prompt cannot be shown.")
                }
            }
            AuthStatus.AUTH_ERROR -> {
                viewModel.logAuthScreenEvent("Auth error received. Re-triggering prompt.")
                val activity = context.findActivity()
                if (activity != null) {
                    viewModel.triggerBiometricPrompt(activity)
                } else {
                    viewModel.logAuthScreenEvent("ERROR: Activity was null on auth error. Cannot re-trigger.")
                }
            }
            AuthStatus.LOADING -> {
                viewModel.logAuthScreenEvent("Auth state is LOADING.")
            }
        }
    }

    // The UI for the AuthScreen.
    // For now, it's just a loading spinner.
    // You could easily add your app's logo here.
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (authStatus == AuthStatus.LOADING || authStatus == AuthStatus.BIOMETRIC_REQUIRED) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}