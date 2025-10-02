package com.kpr.fintrack.utils.parsing.bankPatterns

import com.kpr.fintrack.utils.parsing.ParsingPatternRepository
import java.util.regex.Pattern

object BOIPatterns {
    private  val debitPattern = listOf(
        ParsingPatternRepository.TransactionPattern(
            id = "boi_upi_debit_v1",
            bankName = "BOI",
            regex = Pattern.compile(
                """Rs.([\d,]+\.\d{2})\s+?debited\s+?A/c(\w+)\s?and\s?credited\s?to\s?(.+?)via\s?UPI\s?Ref\s?No\s?(\d+)\s?on\s?(\w+).""",
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 1,
            accountGroup = 2,
            dateGroup = 5,
            merchantGroup = 3,
            referenceGroup = 4,
            transactionType = "debit",
            baseConfidence = 0.95f
        )
    )

    private val creditPattern = listOf(
        ParsingPatternRepository.TransactionPattern(
            id = "boi_upi_credit_v1",
            bankName = "BOI",
            regex = Pattern.compile(
                """BOI\s+?-\s+?Rs.([\d,]+\.\d{2})\s?Credited\s?to\s?your\s?Ac\s(\w+)\son\s?(\d{2}-\d{2}-\d{2})\s?by\s?UPI\s?ref\s?No.(\d+).""",
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 1,
            accountGroup = 2,
            dateGroup = 3,
            merchantGroup = 0,
            referenceGroup = 4,
            transactionType = "credit",
            baseConfidence = 0.95f
        )

    )

    fun getBOIPatterns(): List<ParsingPatternRepository.TransactionPattern> {
        return listOf(debitPattern, creditPattern).flatten()
    }
}