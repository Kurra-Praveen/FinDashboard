package com.kpr.fintrack.utils.parsing.bankPatterns

import com.kpr.fintrack.utils.parsing.ParsingPatternRepository.TransactionPattern
import java.util.regex.Pattern

object  HDFCPatterns {

    private val debitPattern= listOf(
        TransactionPattern(
            id = "hdfc_debit_alert_v1",
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

        ),


        TransactionPattern(
            id = "hdfc_card_upi_txn",
            bankName = "HDFC",
            regex = Pattern.compile(
                """Txn Rs\.([\d.]+)\s+On\s+(.+?)\s+Card\s+(\d+)\s+At\s+([\w.@]+)\s+by UPI\s+(\d+)\s+On\s+(\d{2}-\d{2})"""
            ),
            amountGroup = 1,
            accountGroup = 3,
            merchantGroup = 4,
            referenceGroup = 5,
            dateGroup = 6,
            transactionType = "debit",
            baseConfidence = 0.9f
        ),
        TransactionPattern(
            id = "hdfc_atm_withdrawal_v2",
            bankName = "HDFC",
            regex = Pattern.compile(
                "Withdrawn Rs\\.(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?) From HDFC Bank Card x(\\d{4})" +
                        "\\s*At ([A-Z\\s]+) On (\\d{4}-\\d{2}-\\d{2}:\\d{2}:\\d{2}:\\d{2})" +
                        "\\s*Bal Rs\\.(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)",
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 1,
            accountGroup = 2,
            merchantGroup = 3,
            dateGroup = 4,
            transactionType = "debit",
            baseConfidence = 0.98f,
        )

    )
    private val creditPatterns=listOf(
        TransactionPattern(
            id = "hdfc_credit_alert_v1",
            bankName = "HDFC",
            regex = Pattern.compile(
                """Rs\.([\d.]+)\s+credited to\s+(.+?)\s+A/c\s+(\w+)\s+on\s+(\d{2}-\d{2}-\d{2})\s+from VPA\s+([\w.@]+)\s+\(UPI\s+(\d+)\)""",
                Pattern.CASE_INSENSITIVE or Pattern.DOTALL
            ),
            amountGroup = 1,
            accountGroup = 3,
            dateGroup = 4,
            merchantGroup = 5,
            referenceGroup = 6,
            transactionType = "credit",
            baseConfidence = 0.95f

        )
    )




    fun getHDFCPatterns(): List<TransactionPattern> {
        return listOf(debitPattern, creditPatterns).flatten()
    }

}