package com.kpr.fintrack.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kpr.fintrack.R
import com.kpr.fintrack.data.datasource.ImageProcessingResult
import com.kpr.fintrack.data.datasource.ImageReceiptDataSource
import com.kpr.fintrack.domain.model.Account
import com.kpr.fintrack.domain.model.createTransactionFromParseResult
import com.kpr.fintrack.domain.repository.TransactionRepository
import com.kpr.fintrack.utils.FinTrackLogger
import com.kpr.fintrack.utils.logging.SecureLogger
import com.kpr.fintrack.utils.parsing.BankUtils
import com.kpr.fintrack.utils.parsing.CategoryMatcher
import com.kpr.fintrack.utils.parsing.ImageTransactionParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class ImageImportService : Service() {
    @Inject
    lateinit var imageReceiptDataSource: ImageReceiptDataSource

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var imageTransactionParser: ImageTransactionParser

    @Inject
    lateinit var accountRepository: com.kpr.fintrack.domain.repository.AccountRepository

    @Inject
    lateinit var categoryMatcher: CategoryMatcher

    @Inject
    lateinit var secureLogger: SecureLogger
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        FinTrackLogger.Receipt.logServiceEvent("Service onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val imageUri =
            intent?.getParcelableExtra(EXTRA_IMAGE_URI, Uri::class.java)

        if (imageUri == null) {
            FinTrackLogger.e(TAG, "No image URI provided in intent")
            return START_NOT_STICKY
        }

        FinTrackLogger.Receipt.logServiceEvent("Service started", "URI: $imageUri")
        // Start foreground service with processing notification
        startForeground(NOTIFICATION_ID, createProgressNotification())

        serviceScope.launch {
            processImage(imageUri)
        }

        return START_REDELIVER_INTENT
    }

    private suspend fun processImage(imageUri: Uri) {
        try {
            FinTrackLogger.Receipt.logServiceEvent("Processing image", "URI: $imageUri")
            when (val result = imageReceiptDataSource.processReceipt(imageUri)) {
                is ImageProcessingResult.Success -> {
                    FinTrackLogger.Receipt.logServiceEvent(
                        "OCR completed", "Text length: ${result.text.length}"
                    )

                    try {
                        val parseResult = imageTransactionParser.parseText(result.text, imageUri)

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

                                val account=parseResult.accountNumber?.let { accountRepository.getOrCreateAccount(parseResult.accountNumber,null,null) }
                                val receiptSource = BankUtils.determineReceiptSource(result.text)

//                                val transaction = com.kpr.fintrack.domain.model.Transaction(
//                                    amount = parseResult.amount ?: return,
//                                    isDebit = parseResult.isDebit ?: true,
//                                    merchantName = parseResult.merchantName ?: "Unknown",
//                                    description = parseResult.description ?: result.text,
//                                    category = category,
//                                    date = parseResult.extractedDate ?: LocalDateTime.now(),
//                                    upiApp = parseResult.upiApp,
//                                    accountNumber = parseResult.accountNumber,
//                                    referenceId = parseResult.referenceId,
//                                    smsBody = result.text.replace(Regex("\\s+"), " ") // Normalize whitespace
//                                        .trim(),
//                                    sender = "Extracted from UPI Receipt ${parseResult.upiApp?.name}",
//                                    confidence = parseResult.confidence,
//                                    account = account,
//                                    receiptImagePath = result.savedFilePath,
//                                    receiptSource = receiptSource
//                                )
                                val transaction=createTransactionFromParseResult(
                                    parseResult,
                                    category,
                                    account,
                                    result.text,
                                    "Extracted from UPI Receipt ${parseResult.upiApp?.name}",
                                    receiptSource,
                                    result.savedFilePath,
                                    receiptSource=receiptSource
                                )
                                transaction?.let {
                                    FinTrackLogger.d(TAG, "Inserting transaction : $it")
                                    FinTrackLogger.d(TAG, "Inserting transaction with receipt path: ${result.savedFilePath}")
                                    transactionRepository.insertTransaction(it)
                                    showSuccessNotification()
                                }
                            }
                        } else {
                            FinTrackLogger.w(TAG, "Failed to parse transaction from OCR text")
                            showFailureNotification("Transaction Already exists or Low confidence in parsing")
                        }

                    } catch (e: Exception) {
                        FinTrackLogger.w(TAG, "Failed to parse transaction from OCR text ${e.stackTrace}",e)
                        showFailureNotification("Could not parse transaction details")
                    }
                }

                is ImageProcessingResult.Error -> {
                    FinTrackLogger.e(TAG, "Image processing failed", result.exception)
                    showFailureNotification("Failed to process image: ${result.exception.localizedMessage}")
                }
            }
        } catch (e: Exception) {
            FinTrackLogger.e(TAG, "Unexpected error in processImage", e)
            showFailureNotification("Unexpected error: ${e.localizedMessage}")
        } finally {
            FinTrackLogger.Receipt.logServiceEvent("Processing completed")
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Receipt Processing", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows receipt processing progress"
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createProgressNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("Processing Receipt")
            .setProgress(0, 0, true).setSmallIcon(R.drawable.ic_stop).setOngoing(true).build()

    private fun showSuccessNotification() {
        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("Receipt Processed")
                .setContentText("Transaction added successfully").setSmallIcon(R.drawable.ic_stop)
                .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showFailureNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Receipt Processing Failed").setContentText(message)
            .setSmallIcon(R.drawable.ic_stop).build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private suspend fun getOrCreateAccountByNumber(accountNumber: String?): Account? {
        val account = accountNumber?.let { accountNumber ->
            try {
                // First try to find existing account
                val existingAccount = accountRepository.getAccountByNumber(accountNumber).first()
                if (existingAccount != null) {
                    existingAccount
                } else {
                    // Create new account if not found
                    secureLogger.i("SMS_RECEIVER", "Account not found for number: $accountNumber, creating new account")
                    //createAccountFromImageReceipt(accountNumber, "Null")
                    accountRepository.createAccountFromSource(accountNumber, null, null)
                }
            } catch (e: Exception) {
                secureLogger.w("SMS_RECEIVER", "Failed to find/create account for number: $accountNumber $e")
                null
            }
        }
        return account
    }


    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        FinTrackLogger.Receipt.logServiceEvent("Service onDestroy")
        serviceJob.cancel()
    }

    companion object {
        private const val TAG = "FinTrack_ImportService"
        private const val CHANNEL_ID = "receipt_processing_channel"
        private const val NOTIFICATION_ID = 1001
        private const val EXTRA_IMAGE_URI = "extra_image_uri"

        fun startService(context: Context, imageUri: Uri) {
            val intent = Intent(context, ImageImportService::class.java).apply {
                putExtra(EXTRA_IMAGE_URI, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startForegroundService(intent)
        }
    }
}
