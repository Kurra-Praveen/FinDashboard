package com.kpr.fintrack.services.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.kpr.fintrack.domain.model.Account
import com.kpr.fintrack.domain.repository.TransactionRepository
import com.kpr.fintrack.utils.parsing.TransactionParser
import com.kpr.fintrack.utils.logging.SecureLogger
import com.kpr.fintrack.utils.parsing.CategoryMatcher
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

    @Inject lateinit var transactionParser: TransactionParser
    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var categoryMatcher: CategoryMatcher
    @Inject lateinit var secureLogger: SecureLogger

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("TransactionSmsReceiver", "onReceive called with action: ${intent.action}")
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
            try {
                val parseResult = transactionParser.parseTransaction(
                    messageBody = message.messageBody,
                    sender = message.displayOriginatingAddress ?: "Unknown",
                    timestamp = LocalDateTime.now()
                )

                if (parseResult.isFinancialTransaction && parseResult.confidence > 0.6f) {
                    // Check if transaction already exists
                    parseResult.referenceId?.let { refId ->
                        val existingTransaction = transactionRepository.getTransactionByReferenceId(refId)
                        if (existingTransaction != null) {
                            secureLogger.d("SMS_RECEIVER", "Transaction already exists with ref: $refId")
                            return@launch
                        }
                    }

                    // Determine category
                    val category = categoryMatcher.findBestCategory(
                        merchantName = parseResult.merchantName ?: "",
                        description = parseResult.description ?: "",
                        upiApp = parseResult.upiApp
                    )
                   // val account= Account()

                    val transaction = com.kpr.fintrack.domain.model.Transaction(
                        amount = parseResult.amount ?: return@launch,
                        isDebit = parseResult.isDebit ?: true,
                        merchantName = parseResult.merchantName ?: "Unknown",
                        description = parseResult.description ?: message.messageBody,
                        category = category,
                        date = parseResult.extractedDate ?:LocalDateTime.now(),
                        upiApp = parseResult.upiApp,
                        accountNumber = parseResult.accountNumber,
                        referenceId = parseResult.referenceId,
                        smsBody = message.messageBody,
                        sender = message.displayOriginatingAddress ?: "Unknown",
                        confidence = parseResult.confidence,
                        account = null
                    )

                    transactionRepository.insertTransaction(transaction)
                    secureLogger.i("SMS_RECEIVER", "New transaction detected and saved")

                    // Optionally send notification to user about new transaction
                    // NotificationHelper.showNewTransactionNotification(context, transaction)
                }

            } catch (e: Exception) {
                secureLogger.e("SMS_RECEIVER", "Error processing SMS", e)
            }
        }
    }
}
