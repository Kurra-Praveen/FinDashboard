package com.kpr.fintrack.utils.parsing

import com.kpr.fintrack.utils.parsing.bankPatterns.HDFCPatterns
import com.kpr.fintrack.utils.parsing.bankPatterns.ICICIPatterns

class Patterns {

    companion object {
        fun getAllPatterns(): List<ParsingPatternRepository.TransactionPattern> {
            return listOf(HDFCPatterns.getHDFCPatterns(), ICICIPatterns.getICICIPatterns()).flatten()
        }
    }

}