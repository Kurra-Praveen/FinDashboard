package com.kpr.fintrack.utils.parsing.bankPatterns

import com.kpr.fintrack.utils.parsing.ParsingPatternRepository
import java.util.regex.Pattern

object SBIPatterns {

    //create a two val debit & credit patterns
    private  val debitPattern= listOf(
        ParsingPatternRepository.TransactionPattern(
            id = "sbi_upi_debit_v1",
            bankName = "SBI",
            regex = Pattern.compile(
                """A/C\s(\w+)\sdebited\s+?by\s+?([\d,]+\.\d{1,2})\s?on\s?date\s(\w+)\strf\s?to\s+?(.+?)\s?Refno\s(\d+)""",
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 2,
            accountGroup = 1,
            dateGroup = 3,
            merchantGroup = 4,
            referenceGroup = 5,
            transactionType = "debit",
            baseConfidence = 0.95f
        )
    )

    fun getSBIPatterns(): List<ParsingPatternRepository.TransactionPattern> {
        return listOf(debitPattern).flatten()
    }
}