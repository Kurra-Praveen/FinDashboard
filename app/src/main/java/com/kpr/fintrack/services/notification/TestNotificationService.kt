package com.kpr.fintrack.services.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kpr.fintrack.R
import com.kpr.fintrack.presentation.ui.MainActivity
import com.kpr.fintrack.utils.logging.SecureLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestNotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureLogger: SecureLogger
) {
    
    companion object {
        private const val CHANNEL_ID = "test_notification_channel"
        private const val NOTIFICATION_ID = 9999
        private const val CHANNEL_NAME = "Test Notifications"
        private const val CHANNEL_DESCRIPTION = "Test notifications for development and debugging"
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun sendTestNotification() {
        try {
            createNotificationChannel()
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("🧪 Test Notification")
                .setContentText("This is a test notification from FinTrack!")
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    "This is a test notification to verify that the notification system is working correctly. " +
                    "If you can see this, notifications are properly configured!"
                ))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, notification)
            
            secureLogger.i("TestNotificationService", "Test notification sent successfully")
            
        } catch (e: Exception) {
            secureLogger.e("TestNotificationService", "Failed to send test notification", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
