// Create in a new file, e.g., /data/manager/UserPreferencesManager.kt

package com.kpr.fintrack.data.manager

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("fintrack_prefs", Context.MODE_PRIVATE)

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled

    fun setBiometricEnabled(isEnabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_BIOMETRIC_ENABLED, isEnabled)
        }
        _isBiometricEnabled.value = isEnabled
    }

    companion object {
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
    }
}