// Create in /presentation/ui/settings/SettingsViewModel.kt

package com.kpr.fintrack.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kpr.fintrack.data.manager.UserPreferencesManager
import com.kpr.fintrack.utils.security.BiometricAuthManager
import com.kpr.fintrack.utils.security.BiometricCapability
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SettingsUiState(
    val biometricCapability: BiometricCapability = BiometricCapability.UNKNOWN,
    val isBiometricEnabled: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    biometricAuthManager: BiometricAuthManager,
    private val prefsManager: UserPreferencesManager
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = prefsManager.isBiometricEnabled
        .map { isEnabled ->
            SettingsUiState(
                biometricCapability = biometricAuthManager.checkCapability(),
                isBiometricEnabled = isEnabled
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState()
        )

    fun onBiometricToggled(isEnabled: Boolean) {
        // We only set the preference. The OS will handle the prompt
        // when we try to authenticate on the new AuthScreen.
        // For *setup*, we should show a prompt to confirm.
        // For this POC, we'll just toggle the preference.
        // A full implementation would show a confirmation prompt here.
        prefsManager.setBiometricEnabled(isEnabled)
    }
}