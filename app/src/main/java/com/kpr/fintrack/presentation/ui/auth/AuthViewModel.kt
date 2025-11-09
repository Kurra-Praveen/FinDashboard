package com.kpr.fintrack.presentation.ui.auth

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kpr.fintrack.data.manager.UserPreferencesManager
import com.kpr.fintrack.utils.FinTrackLogger // Your logger
import com.kpr.fintrack.utils.security.BiometricAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthStatus {
    LOADING,
    BIOMETRIC_REQUIRED,
    NO_AUTH_REQUIRED,
    AUTH_SUCCESS,
    AUTH_ERROR
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val prefsManager: UserPreferencesManager,
    private val biometricAuthManager: BiometricAuthManager,
    private val logger: FinTrackLogger // Injecting your existing logger
) : ViewModel() {

    private val _authStatus = MutableStateFlow(AuthStatus.LOADING)
    val authStatus: StateFlow<AuthStatus> = _authStatus.asStateFlow()

    companion object {
        private const val TAG = "AuthViewModel"
    }

    init {
        checkAuthRequirement()
    }

    private fun checkAuthRequirement() {
        logger.d(TAG, "Checking authentication requirement...") // USE LOGGER
        viewModelScope.launch {
            val isEnabled = try {
                prefsManager.isBiometricEnabled.first()
            } catch (e: Exception) {
                logger.e(TAG, "Error reading biometric preference: ${e.message}", e) // USE LOGGER
                false
            }

            if (isEnabled) {
                _authStatus.value = AuthStatus.BIOMETRIC_REQUIRED
                logger.d(TAG, "Auth status updated: BIOMETRIC_REQUIRED") // USE LOGGER
            } else {
                _authStatus.value = AuthStatus.NO_AUTH_REQUIRED
                logger.d(TAG, "Auth status updated: NO_AUTH_REQUIRED") // USE LOGGER
            }
        }
    }

    fun logAuthScreenEvent(message: String) {
        logger.d("AuthScreen", message)
    }

    fun triggerBiometricPrompt(activity: AppCompatActivity) {
        if (_authStatus.value != AuthStatus.BIOMETRIC_REQUIRED) {
            logger.w(
                TAG,
                "Trigger prompt called in an invalid state: ${_authStatus.value}"
            ) // USE LOGGER
            return
        }

        logger.d(TAG, "Triggering biometric prompt...") // USE LOGGER
        biometricAuthManager.showBiometricPrompt(
            activity = activity,
            onSuccess = {
                logger.d(TAG, "Biometric authentication successful.") // USE LOGGER
                _authStatus.value = AuthStatus.AUTH_SUCCESS
            },
            onError = { errorCode, errString ->
                logger.e(
                    TAG,
                    "Biometric auth error. Code: $errorCode, Message: $errString"
                ) // USE LOGGER
                _authStatus.value = AuthStatus.AUTH_ERROR
            }
        )
    }
}