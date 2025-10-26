package com.kpr.fintrack.utils.parsing.upiPatterns

import com.kpr.fintrack.utils.parsing.ParsingPatternRepository
import java.util.regex.Pattern

object PhonePePatterns {

    private  val creditPattern = listOf(
        ParsingPatternRepository.TransactionPattern(
            id = "phonePe_credit_v1",
            bankName = "PHONEPE",
            regex = Pattern.compile(
                "Transaction\\s+Successful\\s+((\\d{2}:\\d{2}\\s+[ap]m)\\s+on\\s+(\\d{2}\\s+\\w+\\s+\\d{4}))\\s+Received from\\s+(.+?)([\\d.,]+)\\s+?.*?Banking\\sName\\s:(.*?)Transfer.*?Credited\\s+to\\s+[A-WYZa-wyz]?([Xx*]+[\\d]+).*?UTR:\\s+(\\d+)\\s+",
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 5,
            accountGroup = 7,
            dateGroup = 1,
            merchantGroup = 6,
            referenceGroup = 8,
            transactionType = "credit",
            baseConfidence = 0.95f,
            isUpi = true,
            bankNameGroup = 0
        ),
        ParsingPatternRepository.TransactionPattern(
            id = "phonePe_credit_v2",
            bankName = "PHONEPE",
            regex = Pattern.compile(
                "Tran.*sful\\s+((\\d{2}:\\d{2}\\s+[ap]m)\\s+on\\s+(\\d{2}\\s+\\w+\\s+\\d{4}))\\s+Received from\\s+(.+?)([\\d.,]+)\\s+.*Credit.*to\\s+[A-WYZa-wyz]?([Xx*]+[\\d]+).*?UTR:\\s+(\\d+)\\s+",
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 5,
            accountGroup = 6,
            dateGroup = 1,
            merchantGroup = 4,
            referenceGroup = 7,
            transactionType = "credit",
            baseConfidence = 0.95f,
            isUpi = true,
            bankNameGroup = 0
        )
    )
    private val debitPattern = listOf(
        ParsingPatternRepository.TransactionPattern(
            id = "phonePe_debit_v1",
            bankName = "PHONEPE",
            regex = Pattern.compile(
                "Tran.*sful\\s+((\\d{2}:\\d{2}\\s+[ap]m)\\s+on\\s+(\\d{2}\\s+\\w+\\s+\\d{4}))\\s+Paid\\s+to\\s+(.+?)([\\d.,]+)\\s.*Banking\\s+Name\\s+:(.*?)Trans.*Debited\\s+from\\s+[A-WYZa-wyz]?([Xx*]+[\\d]+).*?UTR:\\s+(\\d+)\\s+",  // Placeholder regex
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 5,
            accountGroup = 7,
            dateGroup = 1,
            merchantGroup = 6,
            referenceGroup = 8,
            transactionType = "debit",
            baseConfidence = 0.8f,
            isUpi = true,
            bankNameGroup = 0
        ),
        ParsingPatternRepository.TransactionPattern(
            id = "phonePe_debit_v2",
            bankName = "PHONEPE",
            regex = Pattern.compile(
                "Trans.*ful\\s+((\\d{2}:\\d{2}\\s+[ap]m)\\s+on\\s+(\\d{2}\\s+\\w+\\s+\\d{4}))\\s+Paid\\s+to\\s(.+@?)Payment.*from\\s+[A-WYZa-wyz]?([Xx*]+[\\d]+)(.+?)([\\d.,]+)\\sUTR:\\s+(\\d+)\\s+",
                Pattern.CASE_INSENSITIVE
            ),
            amountGroup = 7,
            accountGroup = 5,
            dateGroup = 1,
            merchantGroup = 4,
            referenceGroup = 8,
            transactionType = "debit",
            baseConfidence = 0.05f,
            isUpi = true,
            bankNameGroup = 0
        )
    )



    fun getPhonePePatterns(): List<ParsingPatternRepository.TransactionPattern> {
        return listOf(creditPattern,debitPattern).flatten()
    }
}