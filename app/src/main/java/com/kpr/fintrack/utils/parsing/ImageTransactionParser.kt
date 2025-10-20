package com.kpr.fintrack.utils.parsing

import android.net.Uri
import com.kpr.fintrack.domain.model.Account
import com.kpr.fintrack.domain.model.UpiApp
import com.kpr.fintrack.domain.repository.AccountRepository
import com.kpr.fintrack.utils.FinTrackLogger
import com.kpr.fintrack.utils.parsing.TransactionParser.ParseResult
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import java.util.regex.Matcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageTransactionParser @Inject constructor(
    private val patternRepository: ParsingPatternRepository) {

    @Inject
    lateinit var accountRepository: AccountRepository
    /**
     * Parses OCR-extracted text from payment app receipts to create a Transaction
     *
     * @param text The OCR-extracted text from the receipt image
     * @return Transaction object if parsing successful, null otherwise
     */
     fun parseText(text: String, uri: Uri): ParseResult {
        FinTrackLogger.d(TAG, "Starting transaction parsing")
        val cleanedText = preprocessText(text)
        FinTrackLogger.d(TAG, "Preprocessed text: $cleanedText")

        var bestResult: ParseResult? = null
        var highestConfidence = 0f
        return when {
            isPhonePeReceipt(uri) -> {
                FinTrackLogger.d(TAG, "Detected PhonePe receipt")
                val upi=UpiApp.getDefaultUpiApps().find { x-> x.name.equals("PhonePe", ignoreCase = true) }
                upi?.let {
                    FinTrackLogger.d(TAG, "Using UPI App: ${it.name}")
                }
                val patterns=patternRepository.getPatternsForSender("PhonePe", upi)

                patterns.forEach { pattern ->
                    val result = tryParseVisualTextWithPattern(cleanedText, pattern,upi)
                    if (result.confidence > highestConfidence) {
                        highestConfidence = result.confidence
                        bestResult = result
                    }
                }
                return bestResult ?: ParseResult.noMatch()

            }
            isGPayReceipt(uri) -> {
                FinTrackLogger.d(TAG, "Detected GPay receipt")
                return ParseResult.noMatch()
            }
            isPaytmReceipt(uri) -> {
                FinTrackLogger.d(TAG, "Detected Paytm receipt")
                return ParseResult.noMatch()
            }
            else -> {
                FinTrackLogger.w(TAG, "Unknown receipt format")
                ParseResult.noMatch()
            }
        }
    }

    private fun preprocessText(text: String): String {
        return text
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()
    }

    private fun isPhonePeReceipt(uri: Uri): Boolean {
        return uri.toString().contains("PhonePe", ignoreCase = true)
    }

    private fun isGPayReceipt(uri: Uri): Boolean {
        return uri.toString().contains("Google", ignoreCase = true)
    }

    private fun isPaytmReceipt(uri: Uri): Boolean {
        return uri.toString().contains("Paytm", ignoreCase = true)
    }

    private fun extractAmount(matcher: Matcher, group: Int): BigDecimal? {
        return try {
            if (group > 0 && group <= matcher.groupCount()) {
                val amountStr = matcher.group(group)?.replace(",", "")?.replace("Rs.", "")?.replace("INR", "")?.trim()
                amountStr?.toBigDecimalOrNull()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractUpiId(text: String): String? {
        return try {
            val upiIdRegex = Regex("([a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+)")
            upiIdRegex.find(text)?.groupValues?.get(1)?.also {
                FinTrackLogger.d(TAG, "Extracted UPI ID: $it")
            }
        } catch (e: Exception) {
            FinTrackLogger.w(TAG, "Failed to extract UPI ID", e)
            null
        }
    }

    private fun extractDate(matcher: Matcher, group: Int): LocalDateTime? {

        val extractedDate=matcher.group(group)?.trim()
        patternRepository.getDateTimeFormatters().forEach { formatter ->
            try {
                val parsedDate = LocalDateTime.parse(extractedDate, formatter)
                FinTrackLogger.d("Local Date", "Extracted date: $parsedDate using formatter: $formatter")
                return parsedDate
            } catch (e: Exception) {
                return LocalDateTime.now()
            }
        }
        return LocalDateTime.now()
    }

    private fun determineTransactionType(message: String, patternType: String?): Boolean {
        val debitKeywords = listOf("debited", "paid", "sent", "withdrawn", "debit", "purchase", "spent")
        val creditKeywords = listOf("credited", "received", "deposit", "credit", "refund", "cashback")

        val lowerMessage = message.lowercase()

        return when {
            debitKeywords.any { lowerMessage.contains(it) } -> true
            creditKeywords.any { lowerMessage.contains(it) } -> false
            patternType?.lowercase() == "debit" -> true
            patternType?.lowercase() == "credit" -> false
            else -> true // Default to debit
        }
    }

    private fun extractReferenceId(matcher:Matcher, group: Int): String? {
        return try {
            if (group >= 0 && group <= matcher.groupCount()) {
                if (group==0){
                    return UUID.randomUUID().toString()
                }
                matcher.group(group)?.trim()
            } else null
        } catch (e: Exception) {
            null
        }
    }
    fun removeAtSubstringOptimized(input: String): String =
        input.indexOf('@').let { atIndex ->
            if (atIndex == -1) input
            else input.substring(0, input.lastIndexOf(' ', atIndex).takeIf { it != -1 } ?: 0)
        }.trim()
    private fun extractMerchantFallback(message: String): String? {
        val merchantPatterns = listOf(
            // Generic patterns
            "at\\s+([A-Z\\s]+)\\s+on".toRegex(),
            "to\\s+([A-Z\\s]+)\\s+on".toRegex(),
            "from\\s+?([A-Za-z0-9 .@]+)".toRegex(),
            "paid\\s+to\\s+([A-Z\\s]+)".toRegex()
        )

        merchantPatterns.forEach { pattern ->
            pattern.find(message)?.let { match ->
                val merchant = match.groupValues[1].trim()
                if (merchant.length > 2) { // Avoid single letters or very short matches
                    return removeAtSubstringOptimized(merchant)
                }
            }
        }
        return null
    }
    private fun extractMerchant(matcher: Matcher, group: Int, message: String): String? {
        return try {
            if (group > 0 && group <= matcher.groupCount()) {
                matcher.group(group)?.trim()
            } else {
                if (group==0){
                    return extractMerchantFallback(message)
                }
                return "Unknown Transaction"
            }
        } catch (e: Exception) {
            null
        }
    }
    private fun extractAccountNumber(matcher: Matcher, group: Int): String? {
        return try {
            if (group > 0 && group <= matcher.groupCount()) {
                matcher.group(group)?.replace("x".toRegex(RegexOption.IGNORE_CASE),"")?.trim()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateConfidence(
        hasAmount: Boolean,
        hasMerchant: Boolean,
        hasReference: Boolean,
        baseConfidence: Float,
        hasUpiApp: Boolean
    ): Float {
        var confidence = baseConfidence

        if (hasAmount) confidence += 0.3f
        if (hasMerchant) confidence += 0.2f
        if (hasReference) confidence += 0.1f
        if (hasUpiApp) confidence += 0.1f

        return confidence.coerceIn(0f, 1f)
    }

    companion object {
        private const val TAG = "FinTrack_Parser"
    }

    private  fun tryParseVisualTextWithPattern(
        visualText: String,
        pattern: ParsingPatternRepository.TransactionPattern,
        upi: UpiApp?
    ): ParseResult {
        val matcher = pattern.regex.matcher(visualText)
        val isMatched = matcher.matches() || matcher.reset().find()

        if (!isMatched) {
            return ParseResult.noMatch()
        }

        try {
            val amount = extractAmount(matcher, pattern.amountGroup)
            val merchantName = extractMerchant(matcher, pattern.merchantGroup, visualText)
            val referenceId = extractReferenceId(matcher, pattern.referenceGroup)
            val accountNumber = extractAccountNumber(matcher, pattern.accountGroup)
            val isDebit = determineTransactionType(visualText, pattern.transactionType)
            val dateReceived=extractDate(matcher, pattern.dateGroup)
            val bankName=extractBankName(matcher, pattern.bankNameGroup,accountNumber)
            var newAccount: Account? =null

            val account = accountNumber?.let {
                runBlocking {
                    accountRepository.getAccountByNumber(it).firstOrNull()
                }
            }
            //Todo: Update bank name for existing accounts
//            if(account?.bankName=="Unknown Account" && bankName != "Unknown Account"){
//                FinTrackLogger.d(TAG, "Mapped to account: ${account.name} of bank: ${account.bankName}")
//                account.bankName=bankName
//                account.let {
//                    runBlocking {
//                        accountRepository.updateAccount(it)
//                    }
//                }
//            }

            if (bankName.equals("Unknown Account", ignoreCase = true) && account==null){
                FinTrackLogger.d(TAG, "Account could not be determined")
                // Create account name from bank name and last 4 digits
                val accountName = accountNumber?.length?.let {
                    if (it >= 4) {
                        "$bankName ****${accountNumber.takeLast(4)}"
                    } else {
                        "$bankName Account"
                    }
                }
                 newAccount = Account(
                    name = accountName?:"Account",
                    accountNumber = accountNumber?:"Unknown",
                    bankName = bankName,
                    accountType = Account.AccountType.SAVINGS, // Default to SAVINGS
                    isActive = true,
                    icon = getBankIcon(bankName),
                    color = getBankColor(bankName)
                )
                newAccount.let {
                    runBlocking {
                        val account=accountRepository.insertAccount(it)
                        newAccount.copy(account)
                    }
                }
            }

            val confidence = calculateConfidence(
                amount != null,
                merchantName != null,
                referenceId != null,
                pattern.baseConfidence,
                upi != null
            )
            return ParseResult(
                isFinancialTransaction = true,
                amount = amount,
                isDebit = isDebit,
                merchantName = merchantName ?: "Unknown",
                description = visualText,
                referenceId = referenceId,
                accountNumber = accountNumber,
                upiApp = upi,
                confidence = confidence,
                extractedDate = dateReceived,
                account = account?:newAccount,
            )

        } catch (e: Exception) {
            return ParseResult.noMatch()
        }
    }

    private fun extractBankName(
        matcher: Matcher,
        bankNameGroup: Int,
        accountNumber: String?
    ): String {
        return try {
            if (bankNameGroup > 0 && bankNameGroup <= matcher.groupCount()) {
                matcher.group(bankNameGroup)?.trim() ?: "Unknown Account"
            } else {
                accountNumber?.let {
                    runBlocking {
                        accountRepository.getAccountByNumber(it).firstOrNull()?.bankName
                    }
                } ?: "Unknown Account"
            }
        } catch (e: Exception) {
            "Unknown Account"
        }
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
