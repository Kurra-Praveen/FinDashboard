package com.kpr.fintrack.utils.parsing.bankPatterns


import com.kpr.fintrack.utils.parsing.ParsingPatternRepository.TransactionPattern
import java.util.regex.Pattern

object ICICIPatterns {

    private val debitPattern=listOf(
        TransactionPattern(
            id = "icici_upi_debit_v1",
            bankName = "ICICI",
            regex = Pattern.compile(
                """Acct\s+(\w+)\s+debited for\s+(Rs\.?|INR)\s?([\d,]+\.\d{2})\s+on\s+(\d{2}-\w{3}-\d{2})(,|;)\s+(.+?)\s+credited\. UPI:(\d+)""",
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 3,
            accountGroup = 1,
            dateGroup = 4,
            merchantGroup = 6,
            referenceGroup = 7,
            transactionType = "debit",
            baseConfidence = 0.95f
        ),
        TransactionPattern(
            id = "icici_card_txn",
            bankName = "ICICI",
            regex = Pattern.compile(
                """(Rs\.?|INR)\s?([\d,]+\.\d{2})\s+spent (on|using)\s+(.+?)\s+Card\s+(\w+)\s+on\s+(\d{2}-\w{3}-\d{2})\s+(at|on)\s+(.+?)\. Avl (Lmt|Limit):\s+(Rs\.?|INR)\s?([\d,]+\.\d{2})"""
            ),
            amountGroup = 2,
            accountGroup = 5,
            dateGroup = 6,
            merchantGroup = 8,
            baseConfidence = 0.9f,
            transactionType = "debit",
            referenceGroup = 0
        ),
        TransactionPattern(
            id = "icici_atm_withdrawal_v2",
            bankName = "ICICI",
            regex = Pattern.compile(
                "ICICI Bank Acc XX(\\d{3,4}) debited Rs\\. ([\\d,]+\\.\\d{2})" +
                        "\\s*on (\\d{2}-[A-Za-z]{3}-\\d{2}) NFS\\*CASH WDL\\*\\." +
                        "\\s*Avb Bal Rs\\. ([\\d,]+\\.\\d{2})\\.",
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 2,
            accountGroup = 1,
            dateGroup = 3,
            transactionType = "debit",
            baseConfidence = 0.98f,
            referenceGroup = 0
        )
    )

    private val creditPattern=listOf(
        TransactionPattern(
            id = "icici_upi_credit_v2",
            bankName = "ICICI",
            regex = Pattern.compile(
                "Dear Customer, Acct XX(\\d{3,4}) is credited with Rs (\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)" +
                        "\\s*on (\\d{2}-[A-Za-z]{3}-\\d{2}) from ([A-Z\\s]+)\\." +
                        "\\s*UPI:(\\d+)-ICICI Bank\\.",
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 2,
            accountGroup = 1,
            dateGroup = 3,
            merchantGroup = 4,
            referenceGroup = 5,
            transactionType = "credit",
            baseConfidence = 0.98f
        )
    )

    fun getICICIPatterns(): List<TransactionPattern> {
        return listOf(debitPattern,creditPattern).flatten()
    }
}