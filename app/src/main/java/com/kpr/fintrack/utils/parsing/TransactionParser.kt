package com.kpr.fintrack.utils.parsing

import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.model.UpiApp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionParser @Inject constructor(
    private val patternRepository: ParsingPatternRepository
) {
        init {
            android.util.Log.d("TransactionParser", "Initialized")
        }

    data class ParseResult(
        val isFinancialTransaction: Boolean,
        val amount: BigDecimal? = null,
        val isDebit: Boolean? = null,
        val merchantName: String? = null,
        val description: String? = null,
        val referenceId: String? = null,
        val accountNumber: String? = null,
        val upiApp: UpiApp? = null,
        val suggestedCategory: Category? = null,
        val confidence: Float = 0f,
        val extractedDate: LocalDateTime? = null
    ) {
        companion object {
            fun noMatch() = ParseResult(isFinancialTransaction = false)
        }
    }

    suspend fun parseTransaction(
        messageBody: String,
        sender: String,
        timestamp:  LocalDateTime
    ): ParseResult {
        android.util.Log.d("TransactionParser", "parseTransaction called with sender: $sender, timestamp: $timestamp")
        val cleanMessage = messageBody.trim().replace("\\s+".toRegex(), " ")

        // Detect UPI app first
        val detectedUpiApp = detectUpiApp(sender, cleanMessage)

        // Get applicable patterns
        val patterns = patternRepository.getPatternsForSender(sender, detectedUpiApp)

        var bestResult: ParseResult? = null
        var highestConfidence = 0f

        patterns.forEach { pattern ->
            val result = tryParseWithPattern(cleanMessage, pattern, detectedUpiApp, timestamp)
            if (result.confidence > highestConfidence) {
                highestConfidence = result.confidence
                bestResult = result
            }
        }

        return bestResult ?: ParseResult.noMatch()
    }

    private fun tryParseWithPattern(
        message: String,
        pattern: ParsingPatternRepository.TransactionPattern,
        upiApp: UpiApp?,
        timestamp:  LocalDateTime
    ): ParseResult {
        val matcher = pattern.regex.matcher(message)

        if (!matcher.find()) {
            return ParseResult.noMatch()
        }

        try {
            val amount = extractAmount(matcher, pattern.amountGroup)
            val merchantName = extractMerchant(matcher, pattern.merchantGroup, message)
            val referenceId = extractReferenceId(matcher, pattern.referenceGroup)
            val accountNumber = extractAccountNumber(matcher, pattern.accountGroup)
            val isDebit = determineTransactionType(message, pattern.transactionType)
            val dateReceived=extractDateFromText(timestamp.toString())

            val confidence = calculateConfidence(
                amount != null,
                merchantName != null,
                referenceId != null,
                pattern.baseConfidence,
                upiApp != null
            )

            return ParseResult(
                isFinancialTransaction = true,
                amount = amount,
                isDebit = isDebit,
                merchantName = merchantName ?: "Unknown",
                description = message,
                referenceId = referenceId,
                accountNumber = accountNumber,
                upiApp = upiApp,
                confidence = confidence,
                extractedDate = timestamp
            )

        } catch (e: Exception) {
            return ParseResult.noMatch()
        }
    }

    private fun extractAmount(matcher: java.util.regex.Matcher, group: Int): BigDecimal? {
        return try {
            if (group > 0 && group <= matcher.groupCount()) {
                val amountStr = matcher.group(group)?.replace(",", "")?.replace("Rs.", "")?.replace("INR", "")?.trim()
                amountStr?.toBigDecimalOrNull()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractMerchant(matcher: java.util.regex.Matcher, group: Int, message: String): String? {
        return try {
            if (group > 0 && group <= matcher.groupCount()) {
                matcher.group(group)?.trim()
            } else {
                // Fallback: extract from common patterns
                extractMerchantFallback(message)
            }
        } catch (e: Exception) {
            null
        }
    }

//    private fun extractMerchantFallback(message: String): String? {
//        val merchantPatterns = listOf(
//            "at\\s+([A-Z\\s]+)\\s+on".toRegex(),
//            "to\\s+([A-Z\\s]+)\\s+on".toRegex(),
//            "from\\s+([A-Z\\s]+)\\s+on".toRegex(),
//            "paid\\s+to\\s+([A-Z\\s]+)".toRegex()
//        )
//
//        merchantPatterns.forEach { pattern ->
//            pattern.find(message)?.let { match ->
//                return match.groupValues[1].trim()
//            }
//        }
//        return null
//    }

    private fun extractReferenceId(matcher: java.util.regex.Matcher, group: Int): String? {
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

    private fun extractAccountNumber(matcher: java.util.regex.Matcher, group: Int): String? {
        return try {
            if (group > 0 && group <= matcher.groupCount()) {
                matcher.group(group)?.replace("x".toRegex(RegexOption.IGNORE_CASE),"")?.trim()
            } else null
        } catch (e: Exception) {
            null
        }
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

    private fun detectUpiApp(sender: String, message: String): UpiApp? {
        val upiApps = UpiApp.getDefaultUpiApps()

        return upiApps.find { upiApp ->
            sender.contains(upiApp.senderPattern, ignoreCase = true) ||
                    message.contains(upiApp.name, ignoreCase = true)
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
    // Add this method to your TransactionParser class:

    private fun extractDateFromText(dateText: String?): LocalDateTime? {
        if (dateText == null) return null

        return try {
            when {
                // Format: dd-MMM-yy (e.g., "06-Sep-25")
                dateText.matches("\\d{2}-[A-Za-z]{3}-\\d{2}".toRegex()) -> {
                    val formatter = DateTimeFormatter.ofPattern("dd-MMM-yy", Locale.ENGLISH)
                    val date = LocalDate.parse(dateText, formatter)
                    date.atStartOfDay()
                }

                // Format: dd/MM/yy (e.g., "21/08/25")
                dateText.matches("\\d{2}/\\d{2}/\\d{2}".toRegex()) -> {
                    val formatter = DateTimeFormatter.ofPattern("dd/MM/yy")
                    val date = LocalDate.parse(dateText, formatter)
                    date.atStartOfDay()
                }

                // Format: dd-MM-yy (e.g., "22-08-25")
                dateText.matches("\\d{2}-\\d{2}-\\d{2}".toRegex()) -> {
                    val formatter = DateTimeFormatter.ofPattern("dd-MM-yy")
                    val date = LocalDate.parse(dateText, formatter)
                    date.atStartOfDay()
                }
                // Format: ISO LocalDateTime (e.g., "2025-09-07T19:55:52")
                dateText.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}".toRegex()) -> {
                    LocalDateTime.parse(dateText, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                }

                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Update the extractMerchantFallback method:
    private fun extractMerchantFallback(message: String): String? {
        val merchantPatterns = listOf(
            // ICICI specific
            "([A-Z\\s]+)\\s+credited".toRegex(),

            // HDFC specific
            "To\\s+([A-Za-z\\s]+)\\s*\\n".toRegex(),

            // Card transactions
            "on\\s+([A-Za-z\\s]+)\\.\\s*Avl".toRegex(),

            // Generic patterns
            "at\\s+([A-Z\\s]+)\\s+on".toRegex(),
            "to\\s+([A-Z\\s]+)\\s+on".toRegex(),
            "from\\s+([A-Z\\s]+)\\s+on".toRegex(),
            "paid\\s+to\\s+([A-Z\\s]+)".toRegex()
        )

        merchantPatterns.forEach { pattern ->
            pattern.find(message)?.let { match ->
                val merchant = match.groupValues[1].trim()
                if (merchant.length > 2) { // Avoid single letters or very short matches
                    return merchant
                }
            }
        }
        return null
    }

}
