package com.kpr.fintrack.utils.extensions

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.YearMonth
import java.time.ZoneOffset

fun LocalDateTime.formatRelativeTime(): String {
    val now = LocalDateTime.now()
    val daysBetween = ChronoUnit.DAYS.between(this.toLocalDate(), now.toLocalDate())

    return when {
        daysBetween == 0L -> "Today"
        daysBetween == 1L -> "Yesterday"
        daysBetween < 7L -> "$daysBetween days ago"
        daysBetween < 30L -> "${daysBetween / 7} week${if (daysBetween / 7 != 1L) "s" else ""} ago"
        else -> this.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    }
}

fun LocalDateTime.toYearMonth(): YearMonth = YearMonth.of(this.year, this.month)

 fun YearMonth.startOfMonth(): LocalDateTime = this.atDay(1).atStartOfDay()
 fun YearMonth.endOfMonth(): LocalDateTime = this.atEndOfMonth().atTime(23, 59, 59)
 fun YearMonth.toTimestamp(): Long = this.atDay(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC)