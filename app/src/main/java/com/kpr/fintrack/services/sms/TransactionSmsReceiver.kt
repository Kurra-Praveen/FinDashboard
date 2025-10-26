package com.kpr.fintrack.services.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.kpr.fintrack.domain.model.Account
import com.kpr.fintrack.domain.repository.TransactionRepository
import com.kpr.fintrack.domain.repository.AccountRepository
import com.kpr.fintrack.utils.parsing.TransactionParser
import com.kpr.fintrack.utils.logging.SecureLogger
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

    @Inject lateinit var transactionParser: TransactionParser
    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var accountRepository: AccountRepository
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

                    // Find or create account by account number
                    val account = parseResult.accountNumber?.let { accountNumber ->
                        try {
                            // First try to find existing account
                            val existingAccount = accountRepository.getAccountByNumber(accountNumber).first()
                            if (existingAccount != null) {
                                if (existingAccount.bankName.equals("Bank", ignoreCase = true)) {
                                    // Update bank name if it's generic
                                    val updatedBankName = extractBankNameFromSms(
                                        message.displayOriginatingAddress ?: "",
                                        message.messageBody
                                    )
                                    val updatedAccount = existingAccount.copy(bankName = updatedBankName)
                                    accountRepository.updateAccount(updatedAccount)
                                    secureLogger.i("SMS_RECEIVER", "Updated account bank name to: $updatedBankName for account number: $accountNumber")
                                    updatedAccount
                                }
                                existingAccount
                            } else {
                                // Create new account if not found
                                secureLogger.i("SMS_RECEIVER", "Account not found for number: $accountNumber, creating new account")
                                createAccountFromSms(accountNumber, parseResult, message)
                            }
                        } catch (e: Exception) {
                            secureLogger.w("SMS_RECEIVER", "Failed to find/create account for number: $accountNumber $e")
                            // Try to create account even if lookup failed
                            try {
                                createAccountFromSms(accountNumber, parseResult, message)
                            } catch (createException: Exception) {
                                secureLogger.e("SMS_RECEIVER", "Failed to create account for number: $accountNumber", createException)
                                null
                            }
                        }
                    }

                    val transaction = com.kpr.fintrack.domain.model.Transaction(
                        amount = parseResult.amount ?: return@launch,
                        isDebit = parseResult.isDebit ?: true,
                        merchantName = parseResult.merchantName ?: "Unknown",
                        description = parseResult.description ?: message.messageBody,
                        category = category,
                        date = parseResult.extractedDate ?: LocalDateTime.now(),
                        upiApp = parseResult.upiApp,
                        accountNumber = parseResult.accountNumber,
                        referenceId = parseResult.referenceId,
                        smsBody = message.messageBody,
                        sender = message.displayOriginatingAddress ?: "Unknown",
                        confidence = parseResult.confidence,
                        account = account
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

    private suspend fun createAccountFromSms(
        accountNumber: String,
        parseResult: TransactionParser.ParseResult,
        message: SmsMessage
    ): Account? {
        return try {
            // Extract bank name from sender or SMS content
            val bankName = extractBankNameFromSms(message.displayOriginatingAddress ?: "", message.messageBody)
            
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
                icon = getBankIcon(bankName),
                color = getBankColor(bankName)
            )

            val accountId = accountRepository.insertAccount(newAccount)
            secureLogger.i("SMS_RECEIVER", "Created new account: $accountName with ID: $accountId")
            
            newAccount.copy(id = accountId)
        } catch (e: Exception) {
            secureLogger.e("SMS_RECEIVER", "Failed to create account from SMS", e)
            null
        }
    }

    private fun extractBankNameFromSms(sender: String, messageBody: String): String {
        // Common bank patterns in SMS senders
        val bankPatterns = mapOf(
            "HDFC" to "HDFC Bank",
            "ICICI" to "ICICI Bank", 
            "SBI" to "State Bank of India",
            "AXIS" to "Axis Bank",
            "KOTAK" to "Kotak Mahindra Bank",
            "PNB" to "Punjab National Bank",
            "BOI" to "Bank of India",
            "BOB" to "Bank of Baroda",
            "CANARA" to "Canara Bank",
            "UNION" to "Union Bank of India"
        )

        val upperSender = sender.uppercase()
        val upperMessage = messageBody.uppercase()

        // Check sender first
        bankPatterns.forEach { (pattern, bankName) ->
            if (upperSender.contains(pattern)) {
                return bankName
            }
        }

        // Check message body
        bankPatterns.forEach { (pattern, bankName) ->
            if (upperMessage.contains(pattern)) {
                return bankName
            }
        }

        // Default fallback
        return "Bank"
    }

    private fun getBankIcon(bankName: String): String {
        return when {
            bankName.contains("HDFC", ignoreCase = true) -> "🏦"
            bankName.contains("ICICI", ignoreCase = true) -> "🏛️"
            bankName.contains("SBI", ignoreCase = true) -> "🏦"
            bankName.contains("AXIS", ignoreCase = true) -> "🏛️"
            bankName.contains("KOTAK", ignoreCase = true) -> "🏦"
            else -> "🏦"
        }
    }

    private fun getBankColor(bankName: String): String {
        return when {
            bankName.contains("HDFC", ignoreCase = true) -> "#FF6B6B"
            bankName.contains("ICICI", ignoreCase = true) -> "#4ECDC4"
            bankName.contains("SBI", ignoreCase = true) -> "#45B7D1"
            bankName.contains("AXIS", ignoreCase = true) -> "#96CEB4"
            bankName.contains("KOTAK", ignoreCase = true) -> "#FFEAA7"
            else -> "#95A5A6"
        }
    }
}
