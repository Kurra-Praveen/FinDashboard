package com.kpr.fintrack.domain.model

data class UpiApp(
    val id: Long = 0,
    val name: String,
    val packageName: String,
    val senderPattern: String, // Regex pattern for SMS sender
    val icon: String
) {
    companion object {
        fun getDefaultUpiApps() = listOf(
            UpiApp(1, "Google Pay", "com.google.android.apps.nbu.paisa.user", "GPAY", "💳"),
            UpiApp(2, "PhonePe", "com.phonepe.app", "PHONEPE", "💜"),
            UpiApp(3, "Paytm", "net.one97.paytm", "PAYTM", "💙"),
            UpiApp(4, "BHIM", "in.gov.uidai.bhim", "BHIM", "🇮🇳"),
            UpiApp(5, "Amazon Pay", "in.amazon.mShop.android.shopping", "AMAZON", "🛒"),
            UpiApp(6, "WhatsApp Pay", "com.whatsapp", "WHATSAPP", "💬")
        )
    }
}
