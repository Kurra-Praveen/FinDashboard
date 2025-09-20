package com.kpr.fintrack.utils

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 提供格式化功能的工具类
 */
object FormatUtils {

    // Always get current locale and formatters at use
    private fun currencyFormatter(): NumberFormat =
        NumberFormat.getCurrencyInstance(Locale.getDefault())

    private fun dateFormatter(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())

    private fun shortDateFormatter(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())

    /**
     * 将BigDecimal格式化为货币字符串
     */
    fun BigDecimal.formatAsCurrency(): String =
        currencyFormatter().format(this)

    /**
     * 将Double格式化为货币字符串
     */
    fun Double.formatAsCurrency(): String =
        currencyFormatter().format(this)

    /**
     * 将LocalDate格式化为日期字符串
     */
    fun LocalDate.formatDate(): String =
        this.format(dateFormatter())

    /**
     * 将LocalDateTime格式化为日期字符串
     */
    fun LocalDateTime.formatDate(): String =
        this.format(dateFormatter())

    /**
     * 将LocalDate格式化为短日期字符串
     */
    fun LocalDate.formatShortDate(): String =
        this.format(shortDateFormatter())
}
