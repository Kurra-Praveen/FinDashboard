package com.kpr.fintrack.utils.parsing.bankPatterns

import com.kpr.fintrack.utils.parsing.ParsingPatternRepository.TransactionPattern
import java.util.regex.Pattern

object INDUSINDPatterns {

    private val debitPattern=listOf(
        TransactionPattern(
            id = "indusind_upi_debit_v1",
            bankName = "INDUSIND",
            regex = Pattern.compile(
                """A/C\s+\*(\w+)\s+debited\sby\sRs\s([\d,]+\.\d{2})\s+towards\s+(.+?)\.\s?RRN:(\d+)\.""",
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 2,
            accountGroup = 1,
            dateGroup = 0,
            merchantGroup = 3,
            referenceGroup = 4,
            transactionType = "debit",
            baseConfidence = 0.95f
        )
    )

    private val creditPattern=listOf(
        TransactionPattern(
            id = "indusind_upi_credit_v1",
            bankName = "INDUSIND",
            regex = Pattern.compile(
                """A/C\s+\*(\w+)\s+credited\sby\sRs\s([\d,]+\.\d{2})\s+from\s+(.+?)\.\s?RRN:(\d+)\.""",
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 2,
            accountGroup = 1,
            dateGroup = 0,
            merchantGroup = 3,
            referenceGroup = 4,
            transactionType = "credit",
            baseConfidence = 0.95f
        )
    )
    fun getIndusindPatterns(): List<TransactionPattern> {
        return listOf(debitPattern, creditPattern).flatten()
    }
}