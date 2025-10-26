package com.kpr.fintrack.utils.parsing

object BankUtils {

     fun getBankIcon(bankName: String): String {
        return when {
            bankName.contains("HDFC", ignoreCase = true) -> "🏦"
            bankName.contains("ICICI", ignoreCase = true) -> "🏛️"
            bankName.contains("SBI", ignoreCase = true) -> "🏦"
            bankName.contains("AXIS", ignoreCase = true) -> "🏛️"
            bankName.contains("KOTAK", ignoreCase = true) -> "🏦"
            else -> "🏦"
        }
    }

     fun getBankColor(bankName: String): String {
        return when {
            bankName.contains("HDFC", ignoreCase = true) -> "#FF6B6B"
            bankName.contains("ICICI", ignoreCase = true) -> "#4ECDC4"
            bankName.contains("SBI", ignoreCase = true) -> "#45B7D1"
            bankName.contains("AXIS", ignoreCase = true) -> "#96CEB4"
            bankName.contains("KOTAK", ignoreCase = true) -> "#FFEAA7"
            else -> "#95A5A6"
        }
    }
}