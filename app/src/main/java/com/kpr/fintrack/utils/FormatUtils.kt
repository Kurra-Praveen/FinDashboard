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
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val shortDateFormatter = DateTimeFormatter.ofPattern("dd MMM")
    
    /**
     * 将BigDecimal格式化为货币字符串
     */
    fun BigDecimal.formatAsCurrency(): String {
        return currencyFormatter.format(this)
    }
    
    /**
     * 将Double格式化为货币字符串
     */
    fun Double.formatAsCurrency(): String {
        return currencyFormatter.format(this)
    }
    
    /**
     * 将LocalDate格式化为日期字符串
     */
    fun LocalDate.formatDate(): String {
        return this.format(dateFormatter)
    }
    
    /**
     * 将LocalDateTime格式化为日期字符串
     */
    fun LocalDateTime.formatDate(): String {
        return this.format(dateFormatter)
    }
    
    /**
     * 将LocalDate格式化为短日期字符串
     */
    fun LocalDate.formatShortDate(): String {
        return this.format(shortDateFormatter)
    }
}