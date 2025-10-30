package com.kpr.fintrack.services.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.kpr.fintrack.domain.model.Account
import com.kpr.fintrack.domain.model.createTransactionFromParseResult
import com.kpr.fintrack.domain.repository.TransactionRepository
import com.kpr.fintrack.domain.repository.AccountRepository
import com.kpr.fintrack.utils.parsing.TransactionParser
import com.kpr.fintrack.utils.logging.SecureLogger
import com.kpr.fintrack.utils.notification.NotificationHelper
import com.kpr.fintrack.utils.parsing.BankUtils
import com.kpr.fintrack.utils.parsing.CategoryMatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
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
            try {
                val parseResult = transactionParser.parseTransaction(
                    messageBody = message.messageBody,
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
                                accountNumber,
                                message.displayOriginatingAddress,
                                message.messageBody
                            )
                        } catch (e: Exception) {
                            secureLogger.w(
                                "SMS_RECEIVER",
                                "Failed to find/create account for number: $accountNumber $e"
                            )
                            null
                        }
                    }

//                    val transaction = com.kpr.fintrack.domain.model.Transaction(
//                        amount = parseResult.amount ?: return@launch,
//                        isDebit = parseResult.isDebit ?: true,
//                        merchantName = parseResult.merchantName ?: "Unknown",
//                        description = parseResult.description ?: message.messageBody,
//                        category = category,
//                        date = parseResult.extractedDate ?: LocalDateTime.now(),
//                        upiApp = parseResult.upiApp,
//                        accountNumber = parseResult.accountNumber,
//                        referenceId = parseResult.referenceId,
//                        smsBody = message.messageBody,
//                        sender = message.displayOriginatingAddress ?: "Unknown",
//                        confidence = parseResult.confidence,
//                        account = account
//                    )
                    val transaction = createTransactionFromParseResult(
                        parseResult,
                        category,
                        account,
                        message.messageBody,
                        message.displayOriginatingAddress ?: "Unknown"
                    ) ?: return@launch
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

    private suspend fun createAccountFromSms(
        accountNumber: String, parseResult: TransactionParser.ParseResult, message: SmsMessage
    ): Account? {
        return try {
            // Extract bank name from sender or SMS content
            val bankName = BankUtils.extractBankNameFromSms(
                message.displayOriginatingAddress ?: "", message.messageBody
            )

            // Create account name from bank name and last 4 digits
            val accountName = if (accountNumber.length >= 4) {
                "$bankName ****${accountNumber.takeLast(4)}"
            } else {
                "$bankName Account"
            }

            val newAccount = Account(
                name = accountName,
                accountNumber = accountNumber,
                bankName = bankName,
                accountType = Account.AccountType.SAVINGS, // Default to SAVINGS
                isActive = true,
                icon = BankUtils.getBankIcon(bankName),
                color = BankUtils.getBankColor(bankName)
            )

            val accountId = accountRepository.insertAccount(newAccount)
            secureLogger.i("SMS_RECEIVER", "Created new account: $accountName with ID: $accountId")

            newAccount.copy(id = accountId)
        } catch (e: Exception) {
            secureLogger.e("SMS_RECEIVER", "Failed to create account from SMS", e)
            null
        }
    }
}
