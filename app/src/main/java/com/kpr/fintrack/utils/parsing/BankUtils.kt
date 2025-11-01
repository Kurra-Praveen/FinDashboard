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

     fun extractBankNameFromSms(sender: String, messageBody: String): String {
        // Common bank patterns in SMS senders
        val bankPatterns = mapOf(
            "HDFC" to "HDFC Bank",
            "ICICI" to "ICICI Bank",
            "SBI" to "State Bank of India",
            "AXIS" to "Axis Bank",
            "KOTAK" to "Kotak Mahindra Bank",
            "PNB" to "Punjab National Bank",
            "BOI" to "Bank of India",
            "BOB" to "Bank of Baroda",
            "CANARA" to "Canara Bank",
            "UNION" to "Union Bank of India"
        )

        val upperSender = sender.uppercase()
        val upperMessage = messageBody.uppercase()

        // Check sender first
        bankPatterns.forEach { (pattern, bankName) ->
            if (upperSender.contains(pattern)) {
                return bankName
            }
        }

        // Check message body
        bankPatterns.forEach { (pattern, bankName) ->
            if (upperMessage.contains(pattern)) {
                return bankName
            }
        }
        // Default fallback
        return "Bank"
    }
    fun determineReceiptSource(text: String): String {
        return when {
            text.contains("PhonePe", ignoreCase = true) -> "PHONEPE"
            text.contains("Google Pay", ignoreCase = true) -> "GPAY"
            text.contains("Paytm", ignoreCase = true) -> "PAYTM"
            else -> "UNKNOWN"
        }
    }

    object CategoryIconDefaults {
        val defaultIcons = listOf(
            "💰", "🧾", "🏠", "🚗", "🍔", "✈️", "🛒", "🏥",
            "🎁", "👕", "💡", "🎉", "📚", "💻", "📞", "🍿"
        )
    }
}