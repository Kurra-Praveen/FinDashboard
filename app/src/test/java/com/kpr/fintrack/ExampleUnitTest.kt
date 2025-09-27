package com.kpr.fintrack

import android.util.Log
import com.kpr.fintrack.domain.model.UpiApp
import com.kpr.fintrack.utils.parsing.ParsingPatternRepository
import com.kpr.fintrack.utils.parsing.ParsingPatternRepository.TransactionPattern
import com.kpr.fintrack.utils.parsing.Patterns
import com.kpr.fintrack.utils.parsing.TransactionParser.ParseResult
import org.junit.Test

import org.junit.Assert.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
         val patternRepository= ParsingPatternRepository()
        val patterns = Patterns.getAllPatterns()
        val patterns1 = patternRepository.getPatternsForSender("", null)
        val messageBody="Sent Rs.50.00\n" +
                "From HDFC Bank A/C *8696\n" +
                "To LANKADA NAGAMANI\n" +
                "On 01/09/25\n" +
                "Ref 415806086780\n" +
                "Not You?\n" +
                "Call 18002586161/SMS BLOCK UPI to 7308080808\n"
        val cleanMessage = messageBody.trim().replace("\\s+".toRegex(), " ")
        var bestResult: ParseResult? = null
        var highestConfidence = 0f

        val result=TransactionPattern(
            id = "hdfc_credit_alert_v1",
            bankName = "HDFC",
            regex = Pattern.compile(
                """Sent Rs\.([\d.]+)\s+From\s+(.+?)\s+A/C\s+[*]?(\d+)\s+To\s+(.+?)\s+On\s+(\d{2}/\d{2}/\d{2})\s+Ref\s+(\d+)""",
                Pattern.CASE_INSENSITIVE or Pattern.DOTALL
            ),
            amountGroup = 1,
            accountGroup = 3,
            dateGroup = 5,
            merchantGroup = 4,
            referenceGroup = 6,
            transactionType = "debit",
            baseConfidence = 0.95f

        )
        val bestresult = tryParseWithPattern(cleanMessage, result, null, LocalDateTime.now())

//        patterns.forEach { pattern ->
//            val result = tryParseWithPattern(cleanMessage, pattern, null, LocalDateTime.now())
//            if (result.confidence > highestConfidence) {
//                highestConfidence = result.confidence
//                bestResult = result
//            }
//        }
        Log.i("Account number", bestresult.accountNumber.toString())
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
            if (group > 0 && group <= matcher.groupCount()) {
                matcher.group(group)?.trim()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractAccountNumber(matcher: java.util.regex.Matcher, group: Int): String? {
        return try {
            if (group > 0 && group <= matcher.groupCount()) {
                matcher.group(group)?.trim()
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