package com.kpr.fintrack.services.scanner

import android.content.Context
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InboxScannerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
        init {
            android.util.Log.d("InboxScannerManager", "Manager initialized")
        }

    fun startInboxScan() {
        val intent = InboxScannerService.startScanIntent(context)
        ContextCompat.startForegroundService(context, intent)
        android.util.Log.d("InboxScannerManager", "startInboxScan called")
    }

    fun stopInboxScan() {
        val intent = InboxScannerService.stopScanIntent(context)
        context.startService(intent)
        android.util.Log.d("InboxScannerManager", "stopInboxScan called")
    }
}
