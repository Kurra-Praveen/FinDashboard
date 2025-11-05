package com.kpr.fintrack.services.scanner

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kpr.fintrack.R
import com.kpr.fintrack.data.datasource.sms.SmsDataSource
import com.kpr.fintrack.domain.manager.AppNotificationManager
import com.kpr.fintrack.domain.model.Account
import com.kpr.fintrack.domain.model.createTransactionFromParseResult
import com.kpr.fintrack.domain.repository.TransactionRepository
import com.kpr.fintrack.domain.repository.AccountRepository
import com.kpr.fintrack.presentation.ui.MainActivity
import com.kpr.fintrack.utils.logging.SecureLogger
import com.kpr.fintrack.utils.parsing.BankUtils
import com.kpr.fintrack.utils.parsing.CategoryMatcher
import com.kpr.fintrack.utils.parsing.TransactionParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject

data class ScanProgress(
    val current: Int = 0,
    val total: Int = 0,
    val isCompleted: Boolean = false,
    val error: String? = null
)

@AndroidEntryPoint
class InboxScannerService : Service() {
    init {
        android.util.Log.d("InboxScannerService", "Service initialized")
    }

    @Inject
    lateinit var smsDataSource: SmsDataSource

    @Inject
    lateinit var transactionParser: TransactionParser

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var accountRepository: AccountRepository

    @Inject
    lateinit var categoryMatcher: CategoryMatcher

    @Inject
    lateinit var secureLogger: SecureLogger

    @Inject
    lateinit var appNotificationManager: AppNotificationManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null

    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    companion object {
        const val ACTION_START_SCAN = "com.kpr.fintrack.START_INBOX_SCAN"
        const val ACTION_STOP_SCAN = "com.kpr.fintrack.STOP_INBOX_SCAN"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "inbox_scanner_channel"

        fun startScanIntent(context: Context): Intent {
            return Intent(context, InboxScannerService::class.java).apply {
                action = ACTION_START_SCAN
            }
        }

        fun stopScanIntent(context: Context): Intent {
            return Intent(context, InboxScannerService::class.java).apply {
                action = ACTION_STOP_SCAN
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SCAN -> {
                startForegroundService()
                startInboxScan()
            }

            ACTION_STOP_SCAN -> {
                stopInboxScan()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        val notification = createProgressNotification(0, 0)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startInboxScan() {
        if (scanJob?.isActive == true) {
            secureLogger.w("INBOX_SCANNER", "Scan already in progress")
            return
        }

        scanJob = serviceScope.launch {
            try {
                secureLogger.i("INBOX_SCANNER", "Starting inbox scan")

                val messages = smsDataSource.getAllSmsMessages().take(1000)
                _scanProgress.value = ScanProgress(total = messages.size)

                updateNotification(0, messages.size)

                var processedCount = 0
                var successCount = 0

                messages.forEachIndexed { index, smsMessage ->
                    if (!isActive) return@forEachIndexed

                    try {
                        val parseResult = transactionParser.parseTransaction(
                            messageBody = smsMessage.body,
                            sender = smsMessage.sender,
                            timestamp = smsMessage.date
                        )

                        if (parseResult.isFinancialTransaction && parseResult.confidence > 0.6f) {
                            // Check if transaction already exists
                            val existingTransaction = parseResult.referenceId?.let { refId ->
                                transactionRepository.getTransactionByReferenceId(refId)
                            }

                            if (existingTransaction == null) {
                                val category = categoryMatcher.findBestCategory(
                                    merchantName = parseResult.merchantName ?: "",
                                    description = parseResult.description ?: "",
                                    upiApp = parseResult.upiApp
                                )

                                // Find or create account by account number
                                val account = parseResult.accountNumber?.let { accountNumber ->
                                    try {
                                        accountRepository.getOrCreateAccount(
                                            accountNumber, smsMessage.sender, smsMessage.body
                                        )

                                    } catch (e: Exception) {
                                        secureLogger.w(
                                            "INBOX_SCANNER",
                                            "Failed to find/create account for number: $accountNumber $e"
                                        )
                                        null
                                    }
                                }

                                val transaction = createTransactionFromParseResult(
                                    parseResult = parseResult,
                                    category = category,
                                    account = account,
                                    originalText = smsMessage.body,
                                    sender = smsMessage.sender
                                )

                                transaction?.let {
                                    secureLogger.d(
                                        "INBOX_SCANNER",
                                        "Inserting transaction from SMS ID: ${smsMessage.id}, Ref: ${transaction.referenceId}"
                                    )
                                    transactionRepository.insertTransaction(it)
                                    successCount++
                                }
//                                transactionRepository.insertTransaction(transaction)
//                                successCount++
                            }
                        }

                    } catch (e: Exception) {
                        secureLogger.e(
                            "INBOX_SCANNER", "Error processing message at index $index", e
                        )
                    }

                    processedCount++
                    _scanProgress.value =
                        ScanProgress(current = processedCount, total = messages.size)
                    updateNotification(processedCount, messages.size)

                    // Add small delay to prevent overwhelming the system
                    delay(50)
                }

                _scanProgress.value = ScanProgress(
                    current = processedCount, total = messages.size, isCompleted = true
                )

                secureLogger.i(
                    "INBOX_SCANNER",
                    "Inbox scan completed. Processed: $processedCount, Success: $successCount"
                )
                appNotificationManager.showScanCompleteNotification(successCount)

            } catch (e: Exception) {
                secureLogger.e("INBOX_SCANNER", "Error during inbox scan", e)
                _scanProgress.value = ScanProgress(error = e.message ?: "Unknown error")
                appNotificationManager.showScanErrorNotification(e.message ?: "Unknown error")
            } finally {
                stopSelf()
            }
        }
    }

    private fun stopInboxScan() {
        scanJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Inbox Scanner", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows progress of SMS inbox scanning"
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createProgressNotification(current: Int, total: Int): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            0,
            stopScanIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("Scanning SMS Messages")
            .setContentText(if (total > 0) "Processing $current of $total messages" else "Starting scan...")
            .setSmallIcon(R.drawable.ic_launcher_background).setProgress(total, current, total == 0)
            .setOngoing(true).addAction(
                R.drawable.ic_launcher_background, "Stop", stopIntent
            ).build()
    }

    private fun updateNotification(current: Int, total: Int) {
        val notification = createProgressNotification(current, total)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        scanJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
