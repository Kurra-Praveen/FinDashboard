package com.kpr.fintrack.utils.parsing

import com.kpr.fintrack.domain.model.UpiApp
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParsingPatternRepository @Inject constructor() {

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

    private val patterns = listOf(
        // ICICI Bank UPI Debit Patterns
        TransactionPattern(
            id = "icici_upi_debit_v1",
            bankName = "ICICI",
            regex = Pattern.compile(
                "ICICI Bank Acct (XX\\d{3,4}) debited for Rs\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)" +
                        "\\s*on\\s*(\\d{2}-[A-Za-z]{3}-\\d{2});\\s*([A-Z\\s]+)\\s*credited\\.\\s*UPI:(\\d+)",
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 2,
            accountGroup = 1,
            dateGroup = 3,
            merchantGroup = 4,
            referenceGroup = 5,
            transactionType = "debit",
            baseConfidence = 0.95f
        ),

        // HDFC Bank Sent Money (UPI Debit)
        TransactionPattern(
            id = "hdfc_sent_upi_v1",
            bankName = "HDFC",
            regex = Pattern.compile(
                "Sent Rs\\.?\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)\\s*" +
                        "From HDFC Bank A/C \\*(\\d{4})\\s*" +
                        "To ([A-Za-z\\s]+)\\s*" +
                        "On (\\d{2}/\\d{2}/\\d{2})\\s*" +
                        "Ref (\\d+)",
                Pattern.CASE_INSENSITIVE or Pattern.DOTALL
            ),
            amountGroup = 1,
            accountGroup = 2,
            merchantGroup = 3,
            dateGroup = 4,
            referenceGroup = 5,
            transactionType = "debit",
            baseConfidence = 0.95f
        ),

        // HDFC Bank Credit Alert
        TransactionPattern(
            id = "hdfc_credit_alert_v1",
            bankName = "HDFC",
            regex = Pattern.compile(
                "Credit Alert!\\s*" +
                        "Rs\\.?\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)" +
                        "\\s*credited to HDFC Bank A/c XX(\\d{4})" +
                        "\\s*on\\s*(\\d{2}-\\d{2}-\\d{2})" +
                        "\\s*from VPA ([\\w\\d\\-@.]+)" +
                        "\\s*\\(UPI (\\d+)\\)",
                Pattern.CASE_INSENSITIVE or Pattern.DOTALL
            ),
            amountGroup = 1,
            accountGroup = 2,
            dateGroup = 3,
            merchantGroup = 4,
            referenceGroup = 5,
            transactionType = "credit",
            baseConfidence = 0.95f
        ),

        // ICICI Bank Card Transaction
        TransactionPattern(
            id = "icici_card_spent_v1",
            bankName = "ICICI",
            regex = Pattern.compile(
                "INR\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)\\s*spent using ICICI Bank Card XX(\\d{4})" +
                        "\\s*on\\s*(\\d{2}-[A-Za-z]{3}-\\d{2})\\s*on\\s*([A-Za-z\\s]+)\\." +
                        "\\s*Avl Limit: INR\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)",
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 1,
            accountGroup = 2,
            dateGroup = 3,
            merchantGroup = 4,
            transactionType = "debit",
            baseConfidence = 0.95f
        ),

        // Enhanced Generic UPI Patterns
        TransactionPattern(
            id = "generic_upi_debit_enhanced",
            bankName = "UPI",
            regex = Pattern.compile(
                "(?:Rs\\.?|INR)\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)\\s*" +
                        "(?:debited|paid|sent).*?" +
                        "(?:to|at)\\s*([A-Za-z\\s]{3,}).*?" +
                        "(?:UPI|Ref)\\s*:?\\s*(\\w+)",
                Pattern.CASE_INSENSITIVE or Pattern.DOTALL
            ),
            amountGroup = 1,
            merchantGroup = 2,
            referenceGroup = 3,
            transactionType = "debit",
            baseConfidence = 0.75f
        ),

        TransactionPattern(
            id = "generic_upi_credit_enhanced",
            bankName = "UPI",
            regex = Pattern.compile(
                "(?:Rs\\.?|INR)\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)\\s*" +
                        "(?:credited|received).*?" +
                        "(?:from)\\s*([A-Za-z\\s@\\d\\-\\.]{3,}).*?" +
                        "(?:UPI|Ref)\\s*:?\\s*(\\w+)",
                Pattern.CASE_INSENSITIVE or Pattern.DOTALL
            ),
            amountGroup = 1,
            merchantGroup = 2,
            referenceGroup = 3,
            transactionType = "credit",
            baseConfidence = 0.75f
        ),

        // SBI Bank Patterns
        TransactionPattern(
            id = "sbi_debit_enhanced",
            bankName = "SBI",
            regex = Pattern.compile(
                "Rs\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)\\s*debited.*?" +
                        "A/c\\s*.*?(\\d{4}).*?" +
                        "on\\s*(\\d{2}-\\d{2}-\\d{2})\\s*" +
                        "(?:at|to)\\s*([A-Za-z\\s]+).*?" +
                        "(?:Ref|UPI)\\s*(\\w+)",
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 1,
            accountGroup = 2,
            dateGroup = 3,
            merchantGroup = 4,
            referenceGroup = 5,
            transactionType = "debit",
            baseConfidence = 0.9f
        ),

        // Generic ATM Withdrawal (Enhanced)
        TransactionPattern(
            id = "atm_withdrawal_enhanced",
            bankName = "ATM",
            regex = Pattern.compile(
                "(?:Rs\\.?|INR)\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)\\s*" +
                        "(?:withdrawn|cash).*?" +
                        "ATM.*?" +
                        "A/c\\s*.*?(\\d{4}).*?" +
                        "on\\s*(\\d{2}[-/]\\d{2}[-/]\\d{2})",
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 1,
            accountGroup = 2,
            dateGroup = 3,
            merchantGroup = -1, // ATM withdrawals don't have merchants
            transactionType = "debit",
            baseConfidence = 0.9f
        ),

        // Kotak Bank Pattern
        TransactionPattern(
            id = "kotak_debit",
            bankName = "KOTAK",
            regex = Pattern.compile(
                "Rs\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)\\s*debited from Kotak Bank.*?" +
                        "A/c\\s*(\\d{4}).*?" +
                        "to\\s*([A-Za-z\\s]+).*?" +
                        "on\\s*(\\d{2}[/-]\\d{2}[/-]\\d{2}).*?" +
                        "UPI Ref\\s*(\\w+)",
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 1,
            accountGroup = 2,
            merchantGroup = 3,
            dateGroup = 4,
            referenceGroup = 5,
            transactionType = "debit",
            baseConfidence = 0.9f
        ),

        // Axis Bank Pattern
        TransactionPattern(
            id = "axis_debit",
            bankName = "AXIS",
            regex = Pattern.compile(
                "Rs\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)\\s*debited from Axis Bank.*?" +
                        "A/c\\s*.*?(\\d{4}).*?" +
                        "at\\s*([A-Za-z\\s]+).*?" +
                        "on\\s*(\\d{2}-\\w{3}-\\d{4}).*?" +
                        "Ref\\s*(\\w+)",
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 1,
            accountGroup = 2,
            merchantGroup = 3,
            dateGroup = 4,
            referenceGroup = 5,
            transactionType = "debit",
            baseConfidence = 0.9f
        ),

        // Mobile Recharge Pattern
        TransactionPattern(
            id = "mobile_recharge",
            bankName = "UPI",
            regex = Pattern.compile(
                "(?:Rs\\.?|INR)\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)\\s*" +
                        "(?:paid|debited).*?" +
                        "(?:mobile|recharge|prepaid).*?" +
                        "(\\d{10}).*?" +
                        "(?:UPI|Ref)\\s*:?\\s*(\\w+)",
                Pattern.CASE_INSENSITIVE or Pattern.DOTALL
            ),
            amountGroup = 1,
            merchantGroup = 2, // Phone number as merchant
            referenceGroup = 3,
            transactionType = "debit",
            baseConfidence = 0.85f
        ),

        // Electricity Bill Payment
        TransactionPattern(
            id = "electricity_bill_payment",
            bankName = "UPI",
            regex = Pattern.compile(
                "(?:Rs\\.?|INR)\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)\\s*" +
                        "(?:paid|debited).*?" +
                        "(?:APEPDCL|ELECTRICITY|POWER|BILL).*?" +
                        "(?:UPI|Ref)\\s*:?\\s*(\\w+)",
                Pattern.CASE_INSENSITIVE or Pattern.DOTALL
            ),
            amountGroup = 1,
            merchantGroup = -1, // Will be set to "Electricity Bill"
            referenceGroup = 2,
            transactionType = "debit",
            baseConfidence = 0.85f
        )
    )

    suspend fun getPatternsForSender(sender: String, upiApp: UpiApp? = null): List<TransactionPattern> {
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
