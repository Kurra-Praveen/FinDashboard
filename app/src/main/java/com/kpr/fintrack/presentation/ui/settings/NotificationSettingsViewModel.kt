package com.kpr.fintrack.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kpr.fintrack.services.notification.TestNotificationService
import com.kpr.fintrack.utils.notification.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val notificationHelper: NotificationHelper,
    private val testNotificationService: TestNotificationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            val settings = notificationHelper.getNotificationSettings()
            val systemNotificationsEnabled = notificationHelper.areNotificationsEnabled()
            
            _uiState.value = _uiState.value.copy(
                settings = settings,
                systemNotificationsEnabled = systemNotificationsEnabled,
                isLoading = false
            )
        }
    }

    fun setDailyNotificationsEnabled(enabled: Boolean) {
        notificationHelper.setDailySpendingNotificationEnabled(enabled)
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(dailyNotificationsEnabled = enabled)
        )
    }

    fun setNotificationTime(time: String) {
        notificationHelper.setNotificationTimePreference(time)
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(notificationTime = time),
            timeUpdateMessage = "Notification time updated to $time"
        )
        
        // Clear the message after 3 seconds
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _uiState.value = _uiState.value.copy(timeUpdateMessage = "")
        }
    }

    fun setNotificationFrequency(frequency: String) {
        notificationHelper.setNotificationFrequencyPreference(frequency)
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(notificationFrequency = frequency)
        )
    }

    fun setInsightsEnabled(enabled: Boolean) {
        notificationHelper.setNotificationInsightsPreference(enabled)
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(insightsEnabled = enabled)
        )
    }

    fun setComparisonEnabled(enabled: Boolean) {
        notificationHelper.setNotificationComparisonPreference(enabled)
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(comparisonEnabled = enabled)
        )
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        notificationHelper.setNotificationBudgetAlertsPreference(enabled)
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(budgetAlertsEnabled = enabled)
        )
    }
    @androidx.annotation.RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS)
    fun sendTestNotification() {
        viewModelScope.launch  {
            try {
                testNotificationService.sendTestNotification()
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
