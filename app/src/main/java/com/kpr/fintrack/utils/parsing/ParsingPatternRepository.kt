package com.kpr.fintrack.utils.parsing

import com.kpr.fintrack.domain.model.UpiApp
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParsingPatternRepository @Inject constructor()  {

    data class TransactionPattern(
        val id: String,
        val bankName: String,
        val regex: Pattern,
        val amountGroup: Int,
        val merchantGroup: Int = -1,
        val referenceGroup: Int = -1,
        val accountGroup: Int = -1,
        val dateGroup: Int = -1,
        val transactionType: String? = null,
        val baseConfidence: Float = 0.7f,
        val isUpi: Boolean = false,
        val bankNameGroup: Int=-1
    )

    private val patterns = Patterns.getAllPatterns()
    // Get patterns based on sender info and UPI app (if available)
     fun getPatternsForSender(sender: String, upiApp: UpiApp? = null): List<TransactionPattern> {
        return when {
            // Bank-specific patterns first (highest accuracy)
            sender.contains("ICICI", ignoreCase = true) -> {
                patterns.filter { it.bankName == "ICICI" } +
                        patterns.filter { it.bankName == "UPI" }
            }
            sender.contains("HDFC", ignoreCase = true) -> {
                patterns.filter { it.bankName == "HDFC" } +
                        patterns.filter { it.bankName == "UPI" }
            }
            sender.contains("SBI", ignoreCase = true) -> {
                patterns.filter { it.bankName == "SBI" } +
                        patterns.filter { it.bankName == "UPI" }
            }
            sender.contains("KOTAK", ignoreCase = true) -> {
                patterns.filter { it.bankName == "KOTAK" } +
                        patterns.filter { it.bankName == "UPI" }
            }
            sender.contains("AXIS", ignoreCase = true) -> {
                patterns.filter { it.bankName == "AXIS" } +
                        patterns.filter { it.bankName == "UPI" }
            }
            sender.contains("INDUSIND", ignoreCase = true) || sender.contains("INDUSB", ignoreCase = true)-> {
                patterns.filter { it.bankName == "INDUSIND" } +
                        patterns.filter { it.bankName == "UPI" }
            }
            sender.contains("BOI", ignoreCase = true) || sender.contains("BANK OF INDIA", ignoreCase = true) -> {
                patterns.filter { it.bankName == "BOI" } +
                        patterns.filter { it.bankName == "UPI" }
            }

            // UPI App specific patterns
            upiApp != null -> {
                val appPatterns = patterns.filter {
                    it.isUpi
                }
                appPatterns.ifEmpty { patterns.filter { it.id.contains("upiapp",ignoreCase = true) } }
            }

            // Generic UPI and other patterns
            else -> patterns.filter { it.bankName == "UPI" || it.bankName == "ATM" }
        }.sortedByDescending { it.baseConfidence } // Sort by confidence
    }

    fun getAllPatterns(): List<TransactionPattern> = patterns.sortedByDescending { it.baseConfidence }

    // Helper method to get category hints from merchant name
    fun getCategoryHint(merchantName: String): String? {
        return when {
            merchantName.contains("APEPDCL", ignoreCase = true) -> "Bills & Utilities"
            merchantName.contains("HEALTHCARE", ignoreCase = true) -> "Healthcare"
            merchantName.contains("AUTO SERVICES", ignoreCase = true) -> "Transportation"
            merchantName.contains("SUPERMAR", ignoreCase = true) -> "Shopping"
            merchantName.matches("\\d{10}".toRegex()) -> "Bills & Utilities" // Phone numbers usually recharge
            else -> null
        }
    }

     fun getDateTimeFormatters(): List<DateTimeFormatter> {
        return listOf(
            // Format: "09:22 pm on 19 Oct 2025"
            DateTimeFormatter.ofPattern("hh:mm a 'on' dd MMM yyyy"),
            // Format: "09:22 PM on 19 Oct 2025" (uppercase AM/PM)
            DateTimeFormatter.ofPattern("hh:mm a 'on' dd MMM yyyy"),
            // Format: "8:56 am" (only time, uses current date)
            DateTimeFormatter.ofPattern("h:mm a"),
            // Format: "6 Jun 2025" (only date, uses start of day)
            DateTimeFormatter.ofPattern("d MMM yyyy"),
            // Format: "06 Jun 2025" (with leading zero)
            DateTimeFormatter.ofPattern("dd MMM yyyy"),
            // Format: "19-10-2025 09:22 PM"
            DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a"),
            // Format: "2025-10-19 21:22"
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            // Format: "19/10/2025 09:22 PM"
            DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a"),
            // Format: "Oct 19, 2025 09:22 PM"
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"),
            // Format: "19 Oct 2025 09:22 PM"
            DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm a"),
            // Format: "2025-10-19"
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
        )
    }
}
