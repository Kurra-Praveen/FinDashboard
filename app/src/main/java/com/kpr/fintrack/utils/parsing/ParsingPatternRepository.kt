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
//        listOf(
//        // ICICI Bank UPI Debit Patterns
//        TransactionPattern(
//            id = "icici_upi_debit_v1",
//            bankName = "ICICI",
//            regex = Pattern.compile(
//                "ICICI Bank Acct (XX\\d{3,4}) debited for Rs\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)" +
//                        "\\s*on\\s*(\\d{2}-[A-Za-z]{3}-\\d{2});\\s*([A-Z\\s]+)\\s*credited\\.\\s*UPI:(\\d+)",
//                Pattern.CASE_INSENSITIVE
//            ),
//            amountGroup = 2,
//            accountGroup = 1,
//            dateGroup = 3,
//            merchantGroup = 4,
//            referenceGroup = 5,
//            transactionType = "debit",
//            baseConfidence = 0.95f
//        ),
//
//        // HDFC Bank Sent Money (UPI Debit)
//        TransactionPattern(
//            id = "hdfc_sent_upi_v1",
//            bankName = "HDFC",
//            regex = Pattern.compile(
//                "Sent Rs\\.?\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)\\s*" +
//                        "From HDFC Bank A/C \\*(\\d{4})\\s*" +
//                        "To ([A-Za-z\\s]+)\\s*" +
//                        "On (\\d{2}/\\d{2}/\\d{2})\\s*" +
//                        "Ref (\\d+)",
//                Pattern.CASE_INSENSITIVE or Pattern.DOTALL
//            ),
//            amountGroup = 1,
//            accountGroup = 2,
//            merchantGroup = 3,
//            dateGroup = 4,
//            referenceGroup = 5,
//            transactionType = "debit",
//            baseConfidence = 0.95f
//        ),
////        TransactionPattern(
////            id = "hdfc_upi_debit",
////            bankName = "HDFC",
////            regex = Pattern.compile(
////                """Sent Rs\.(\d+\.\d{2})\nFrom HDFC Bank A/C \*(\d{4})\nTo (.+)\nOn (\d{2}/\d{2}/\d{2})\nRef (\d+)"""
////            ),
////            amountGroup = 2,
////            merchantGroup = 4,
////            referenceGroup = 6,
////            accountGroup = 3,
////            dateGroup = 5,
////            transactionType = "debit",
////            baseConfidence = 0.9f
////        ),
//
//        // HDFC Bank Credit Alert
//        TransactionPattern(
//            id = "hdfc_credit_alert_v1",
//            bankName = "HDFC",
//            regex = Pattern.compile(
//                "Credit Alert!\\s*" +
//                        "Rs\\.?\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)" +
//                        "\\s*credited to HDFC Bank A/c XX(\\d{4})" +
//                        "\\s*on\\s*(\\d{2}-\\d{2}-\\d{2})" +
//                        "\\s*from VPA ([\\w\\d\\-@.]+)" +
//                        "\\s*\\(UPI (\\d+)\\)",
//                Pattern.CASE_INSENSITIVE or Pattern.DOTALL
//            ),
//            amountGroup = 1,
//            accountGroup = 2,
//            dateGroup = 3,
//            merchantGroup = 4,
//            referenceGroup = 5,
//            transactionType = "credit",
//            baseConfidence = 0.95f
//        ),
//
//        // ICICI Bank Card Transaction
//        TransactionPattern(
//            id = "icici_card_spent_v1",
//            bankName = "ICICI",
//            regex = Pattern.compile(
//                "INR\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)\\s*spent using ICICI Bank Card XX(\\d{4})" +
//                        "\\s*on\\s*(\\d{2}-[A-Za-z]{3}-\\d{2})\\s*on\\s*([A-Za-z\\s]+)\\." +
//                        "\\s*Avl Limit: INR\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)",
//                Pattern.CASE_INSENSITIVE
//            ),
//            amountGroup = 1,
//            accountGroup = 2,
//            dateGroup = 3,
//            merchantGroup = 4,
//            transactionType = "debit",
//            baseConfidence = 0.95f
//        ),
//        TransactionPattern(
//            id = "hdfc_card_upi_txn",
//            bankName = "HDFC",
//            regex = Pattern.compile(
//                """Txn Rs\.(\d+\.\d{2})\nOn HDFC Bank Card (\d{4})\nAt (.+) \nby UPI (\d+)\nOn (\d{2}-\d{2})"""
//            ),
//            amountGroup = 1,
//            accountGroup = 2,
//            merchantGroup = 3,
//            referenceGroup = 4,
//            dateGroup = 5,
//            transactionType = "CARD",
//            baseConfidence = 0.9f
//        ) ,
//        TransactionPattern(
//            id = "icici_card_txn",
//            bankName = "ICICI",
//            regex = Pattern.compile(
//                """(INR|Rs) ([\d,]+\.\d{2}) spent (?:using )?ICICI Bank Card XX(\d{4}) on (\d{2}-[A-Za-z]{3}-\d{2}) (?:on|at) (.+?) \. Avl L(?:imit|mt): (?:INR |Rs )?([\d,]+\.\d{2})\."""
//            ),
//            amountGroup = 2,
//            accountGroup = 3,
//            dateGroup = 4,
//            merchantGroup = 5,
//            baseConfidence = 0.9f,
//            transactionType = "CARD"
//        )
//
//    )

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
