package com.kpr.fintrack.utils.extensions

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

fun BigDecimal.formatCurrency(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return format.format(this)
}

fun BigDecimal.formatAmount(): String {
    val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
    format.minimumFractionDigits = 0
    format.maximumFractionDigits = 2
    return "₹${format.format(this)}"
}
