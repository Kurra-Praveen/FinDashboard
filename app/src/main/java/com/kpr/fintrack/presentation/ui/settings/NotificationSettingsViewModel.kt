package com.kpr.fintrack.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kpr.fintrack.domain.manager.AppNotificationManager
import com.kpr.fintrack.utils.notification.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val appNotificationManager: AppNotificationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            // (MODIFIED) Get settings from the manager
            appNotificationManager.getNotificationSettings().collect { settings ->
                _uiState.value = _uiState.value.copy(
                    settings = settings,
                    systemNotificationsEnabled = appNotificationManager.areNotificationsEnabled(),
                    isLoading = false
                )
            }
        }
    }

    fun setDailyNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appNotificationManager.setDailySpendingNotificationEnabled(enabled)
            // The flow will update the UI automatically, but we can do it manually for responsiveness
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings.copy(dailyNotificationsEnabled = enabled)
            )
        }
    }

    fun setNotificationTime(time: String) {
        viewModelScope.launch {
            appNotificationManager.setNotificationTimePreference(time)
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings.copy(notificationTime = time),
                timeUpdateMessage = "Notification time updated to $time"
            )

            // Clear the message after 3 seconds
            kotlinx.coroutines.delay(3000)
            _uiState.value = _uiState.value.copy(timeUpdateMessage = "")
        }
    }

    fun setNotificationFrequency(frequency: String) {
        viewModelScope.launch {
            appNotificationManager.setNotificationFrequencyPreference(frequency)
        }
    }

    fun setInsightsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appNotificationManager.setNotificationInsightsPreference(enabled)
        }
    }

    fun setComparisonEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appNotificationManager.setNotificationComparisonPreference(enabled)
        }
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appNotificationManager.setNotificationBudgetAlertsPreference(enabled)
        }
    }
    @androidx.annotation.RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS)
    fun sendTestNotification() {
        viewModelScope.launch {
            try {
                // (MODIFIED) Call the manager
                appNotificationManager.showTestNotification()
                _uiState.value = _uiState.value.copy(
                    testNotificationSent = true,
                    testNotificationMessage = "Test notification sent! Check your notification panel."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    testNotificationSent = false,
                    testNotificationMessage = "Failed to send test notification: ${e.message}"
                )
            }
        }
    }

    data class NotificationSettingsUiState(
        val settings: NotificationHelper.NotificationSettings = NotificationHelper.NotificationSettings(
            dailyNotificationsEnabled = false,
            notificationTime = "20:00",
            notificationFrequency = "daily",
            insightsEnabled = true,
            comparisonEnabled = true,
            budgetAlertsEnabled = true
        ),
        val systemNotificationsEnabled: Boolean = false,
        val isLoading: Boolean = true,
        val testNotificationSent: Boolean = false,
        val testNotificationMessage: String = "",
        val timeUpdateMessage: String = ""
    )
}
