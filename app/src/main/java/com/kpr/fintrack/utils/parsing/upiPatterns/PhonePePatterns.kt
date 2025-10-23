package com.kpr.fintrack.utils.parsing.upiPatterns

import com.kpr.fintrack.utils.parsing.ParsingPatternRepository
import java.util.regex.Pattern

object PhonePePatterns {

    private  val creditPattern = listOf(
        ParsingPatternRepository.TransactionPattern(
            id = "phonePe_credit_v1",
            bankName = "PHONEPE",
            regex = Pattern.compile(
                "Transaction Successful\\s+((\\d{2}:\\d{2}\\s+[ap]m)\\s+on\\s+(\\d{2}\\s+\\w+\\s+\\d{4}))\\s+Received from\\s+(.+?)\\s+(\\+91\\d{10})\\s+Banking Name\\s*:\\s(.+?)\\s+Transfer Details\\s+Transaction ID\\s+([A-Z0-9]+)\\s+Credited to\\s+([Xx*]+[\\d]+)\\s+UTR:\\s+(\\d+)\\s+.*?([\\d.,]+)\\s+[A-Za-z]?([\\d.,]+)",
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 10,
            accountGroup = 8,
            dateGroup = 1,
            merchantGroup = 4,
            referenceGroup = 9,
            transactionType = "credit",
            baseConfidence = 0.95f,
            isUpi = true,
            bankNameGroup = 0
        ),
        ParsingPatternRepository.TransactionPattern(
            id = "phonePe_credit_v2",
            bankName = "PHONEPE",
            regex = Pattern.compile(
                "Transaction Successful\\s+((\\d{2}:\\d{2}\\s+[ap]m)\\s+on\\s+(\\d{2}\\s+\\w+\\s+\\d{4}))\\s+Received from\\s+([A-Za-z0-9 .@]+)Credited to\\s+([Xx*]+[\\d]+)\\s+UTR:\\s+(\\d+)\\s+.*?([\\d.,]+)\\s+([\\d.,]+)",
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 7,
            accountGroup = 5,
            dateGroup = 1,
            merchantGroup = 4,
            referenceGroup = 6,
            transactionType = "credit",
            baseConfidence = 0.95f,
            isUpi = true,
            bankNameGroup = 0
        )
    )
    fun getPhonePePatterns(): List<ParsingPatternRepository.TransactionPattern> {
        return listOf(creditPattern).flatten()
    }
}