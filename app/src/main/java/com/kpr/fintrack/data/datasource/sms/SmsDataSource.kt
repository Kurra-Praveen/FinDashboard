package com.kpr.fintrack.data.datasource.sms

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

data class SmsMessage(
    val id: Long,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val date: LocalDateTime
)

@Singleton
class SmsDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun getAllSmsMessages(): List<SmsMessage> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<SmsMessage>()

        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE
                ),
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressColumn = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyColumn = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateColumn = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val sender = it.getString(addressColumn) ?: "Unknown"
                    val body = it.getString(bodyColumn) ?: ""
                    val timestamp = it.getLong(dateColumn)

                    // Filter for potential financial SMS
                    if (isPotentialFinancialSms(sender, body)) {
                        messages.add(
                            SmsMessage(
                                id = id,
                                sender = sender,
                                body = body,
                                timestamp = timestamp,
                                date = LocalDateTime.ofEpochSecond(
                                    timestamp / 1000,
                                    0,
                                    ZoneOffset.systemDefault().rules.getOffset(
                                        java.time.Instant.ofEpochMilli(timestamp)
                                    )
                                )
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Handle permission or other errors
            throw e
        }

        messages
    }

    private fun isPotentialFinancialSms(sender: String, body: String): Boolean {
        val financialSenders = listOf(
            "HDFC", "SBI", "ICICI", "AXIS", "KOTAK", "YES", "INDUSIND",
            "GPAY", "PHONEPE", "PAYTM", "AMAZON", "BHIM", "MOBIKWIK",
            "FREECHARGE", "PAYPAL", "WHATSAPP"
        )

        val financialKeywords = listOf(
            "debited", "credited", "paid", "received", "withdrawal", "deposit",
            "transaction", "payment", "transfer", "upi", "Rs", "INR", "amount",
            "balance", "account", "bank", "atm"
        )

        val senderMatch = financialSenders.any {
            sender.contains(it, ignoreCase = true)
        }

        val bodyMatch = financialKeywords.any {
            body.contains(it, ignoreCase = true)
        }

        return senderMatch || bodyMatch
    }
}
