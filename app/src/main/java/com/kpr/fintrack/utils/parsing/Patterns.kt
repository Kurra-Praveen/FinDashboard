package com.kpr.fintrack.utils.parsing

import com.kpr.fintrack.utils.parsing.bankPatterns.BOIPatterns
import com.kpr.fintrack.utils.parsing.bankPatterns.HDFCPatterns
import com.kpr.fintrack.utils.parsing.bankPatterns.ICICIPatterns
import com.kpr.fintrack.utils.parsing.bankPatterns.INDUSINDPatterns
import com.kpr.fintrack.utils.parsing.bankPatterns.SBIPatterns
import com.kpr.fintrack.utils.parsing.upiPatterns.PhonePePatterns

class Patterns {

    companion object {
        fun getAllPatterns(): List<ParsingPatternRepository.TransactionPattern> {
            return listOf(
                HDFCPatterns.getHDFCPatterns(),
                ICICIPatterns.getICICIPatterns(),
                INDUSINDPatterns.getIndusindPatterns(),
                BOIPatterns.getBOIPatterns(),
                SBIPatterns.getSBIPatterns(),
                PhonePePatterns.getPhonePePatterns()
            ).flatten()
        }
    }

}