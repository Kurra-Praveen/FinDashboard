// Create in a new file, e.g., /utils/security/BiometricAuthManager.kt

package com.kpr.fintrack.utils.security

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.kpr.fintrack.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class BiometricCapability {
    READY,
    NOT_ENROLLED,
    UNSUPPORTED,
    UNKNOWN
}

@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val biometricManager = BiometricManager.from(context)

    fun checkCapability(): BiometricCapability {
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.READY
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapability.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricCapability.UNSUPPORTED
            else -> BiometricCapability.UNKNOWN
        }
    }

    fun showBiometricPrompt(
        activity: AppCompatActivity, // The prompt requires an Activity
        onSuccess: () -> Unit,
        onError: (Int, String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.app_name)) // Use your app name string
            .setSubtitle("Confirm your identity to proceed")
            // CRITICAL: Allow device PIN/Pattern as a fallback.
            // This is essential as biometrics can fail.
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Don't report an error if the user just pressed "cancel"
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        onError(errorCode, errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Called when biometric is valid but not recognized
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }
}