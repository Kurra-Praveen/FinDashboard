package com.kpr.fintrack.services.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.kpr.fintrack.domain.repository.TransactionRepository
import com.kpr.fintrack.utils.logging.SecureLogger
import com.kpr.fintrack.utils.parsing.CategoryMatcher
import com.kpr.fintrack.utils.parsing.TransactionParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class TransactionNotificationListenerService : NotificationListenerService() {
    init {
        android.util.Log.d("TransactionNotificationListenerService", "Service initialized")
    }

    @Inject lateinit var transactionParser: TransactionParser
    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var categoryMatcher: CategoryMatcher
    @Inject lateinit var secureLogger: SecureLogger

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        android.util.Log.d("TransactionNotificationListenerService", "onNotificationPosted called")
        super.onNotificationPosted(sbn)

        sbn?.let { notification ->
            processNotification(notification)
        }
    }

    private fun processNotification(sbn: StatusBarNotification) {
        serviceScope.launch {
            try {
                val packageName = sbn.packageName
                val notification = sbn.notification
                val extras = notification.extras

                val title = extras.getCharSequence("android.title")?.toString() ?: ""
                val text = extras.getCharSequence("android.text")?.toString() ?: ""
                val bigText = extras.getCharSequence("android.bigText")?.toString() ?: text

                // Check if it's from a financial app
                if (isFinancialApp(packageName)) {
                    val messageBody = "$title $bigText".trim()

                    val parseResult = transactionParser.parseTransaction(
                        messageBody = messageBody,
                        sender = packageName,
                        timestamp = LocalDateTime.now()
                    )

                    if (parseResult.isFinancialTransaction && parseResult.confidence > 0.7f) {
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

                            val transaction = com.kpr.fintrack.domain.model.Transaction(
                                amount = parseResult.amount ?: return@launch,
                                isDebit = parseResult.isDebit ?: true,
                                merchantName = parseResult.merchantName ?: "Unknown",
                                description = parseResult.description ?: messageBody,
                                category = category,
                                date = parseResult.extractedDate ?: LocalDateTime.now(),
                                upiApp = parseResult.upiApp,
                                accountNumber = parseResult.accountNumber,
                                referenceId = parseResult.referenceId,
                                smsBody = messageBody,
                                sender = packageName,
                                confidence = parseResult.confidence
                            )

                            transactionRepository.insertTransaction(transaction)
                            secureLogger.i("NOTIFICATION_LISTENER", "Transaction detected from notification")
                        }
                    }
                }

            } catch (e: Exception) {
                secureLogger.e("NOTIFICATION_LISTENER", "Error processing notification", e)
            }
        }
    }

    private fun isFinancialApp(packageName: String): Boolean {
        val financialApps = listOf(
            "com.google.android.apps.nbu.paisa.user", // Google Pay
            "com.phonepe.app", // PhonePe
            "net.one97.paytm", // Paytm
            "in.amazon.mShop.android.shopping", // Amazon Pay
            "com.whatsapp", // WhatsApp Pay
            "in.gov.uidai.bhim" // BHIM
        )

        return financialApps.any { packageName.contains(it, ignoreCase = true) }
    }

    override fun onDestroy() {
        serviceScope.cancel("Cancelling...")
        super.onDestroy()
    }
}
