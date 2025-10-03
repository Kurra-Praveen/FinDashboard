package com.kpr.fintrack.utils.parsing

import com.kpr.fintrack.domain.model.UpiApp
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
        val baseConfidence: Float = 0.7f
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
                    it.bankName == upiApp.name.uppercase() || it.bankName == "UPI"
                }
                appPatterns.ifEmpty { patterns.filter { it.bankName == "UPI" } }
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
}
