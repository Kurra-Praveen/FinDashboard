package com.kpr.fintrack.services.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.kpr.fintrack.domain.manager.AppNotificationManager
import com.kpr.fintrack.domain.model.createTransactionFromParseResult
import com.kpr.fintrack.domain.repository.AccountRepository
import com.kpr.fintrack.domain.repository.TransactionRepository
import com.kpr.fintrack.utils.FormatUtils
import com.kpr.fintrack.utils.logging.SecureLogger
import com.kpr.fintrack.utils.parsing.CategoryMatcher
import com.kpr.fintrack.utils.parsing.TransactionParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class TransactionSmsReceiver : BroadcastReceiver() {
    init {
        android.util.Log.d("TransactionSmsReceiver", "Receiver initialized")
    }

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

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d(
            "TransactionSmsReceiver", "onReceive called with action: ${intent.action}"
        )
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        messages.forEach { message ->
            processMessageAsync(message)
        }
    }

    private fun processMessageAsync(message: SmsMessage) {
        scope.launch {
            val messageBody = message.messageBody ?: "" // (NEW) Define for catch block
            try {
                val parseResult = transactionParser.parseTransaction(
                    messageBody = messageBody,
                    sender = message.displayOriginatingAddress ?: "Unknown",
                    timestamp = LocalDateTime.now()
                )

                if (parseResult.isFinancialTransaction && parseResult.confidence > 0.6f) {
                    // Check if transaction already exists
                    parseResult.referenceId?.let { refId ->
                        val existingTransaction =
                            transactionRepository.getTransactionByReferenceId(refId)
                        if (existingTransaction != null) {
                            secureLogger.d(
                                "SMS_RECEIVER", "Transaction already exists with ref: $refId"
                            )
                            return@launch
                        }
                    }

                    // Determine category
                    val category = categoryMatcher.findBestCategory(
                        merchantName = parseResult.merchantName ?: "",
                        description = parseResult.description ?: "",
                        upiApp = parseResult.upiApp
                    )

                    // Find or create account by account number
                    val account = parseResult.accountNumber?.let { accountNumber ->
                        try {
                            accountRepository.getOrCreateAccount(
                                accountNumber, message.displayOriginatingAddress, messageBody
                            )
                        } catch (e: Exception) {
                            secureLogger.w(
                                "SMS_RECEIVER",
                                "Failed to find/create account for number: $accountNumber $e"
                            )
                            null
                        }
                    }

                    val transaction = createTransactionFromParseResult(
                        parseResult,
                        category,
                        account,
                        messageBody,
                        message.displayOriginatingAddress ?: "Unknown"
                    ) ?: return@launch
                    // (MODIFIED) Get the new transaction ID
                    val newTransactionId = transactionRepository.insertTransaction(transaction)
                    secureLogger.i("SMS_RECEIVER", "New transaction detected and saved")

                    // (NEW) Send SUCCESS notification
                    appNotificationManager.showTransactionAddedNotification(
                        transactionId = newTransactionId,
                        merchantName = transaction.merchantName,
                        amount = FormatUtils.formatCurrency(transaction.amount)
                    )

                } else {
                    // (NEW) Send FAILED notification (low confidence or not financial)
                    secureLogger.w("SMS_RECEIVER", "Parse failed (low confidence): $messageBody")
                    appNotificationManager.showTransactionFailedNotification(originalText = messageBody)
                }

            } catch (e: Exception) {
                secureLogger.e("SMS_RECEIVER", "Error processing SMS", e)
                if (messageBody.isNotEmpty()) { // Only notify if we have a message to show
                    appNotificationManager.showTransactionFailedNotification(originalText = messageBody)
                }
            }
        }
    }
}
