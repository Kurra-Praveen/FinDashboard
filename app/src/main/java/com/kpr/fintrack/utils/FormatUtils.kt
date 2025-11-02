package com.kpr.fintrack.utils

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A utility object that provides functions for formatting various data types,
 * such as currencies and dates, based on the user's current locale.
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
     * Formats a BigDecimal as a currency string based on the user's default locale.
     * For example, in the US locale, 1234.56 would be formatted as "$1,234.56".
     *
     * @return A locale-specific currency representation of the BigDecimal.
     */
    fun BigDecimal.formatAsCurrency(): String =
        currencyFormatter().format(this)

    /**
     * Formats a Double as a currency string based on the user's default locale.
     *
     * For example, in the US locale, 1234.56 becomes "$1,234.56".
     * In the German locale, it becomes "1.234,56 €".
     *
     * @receiver The Double value to be formatted.
     * @return A string representing the formatted currency value.
     */
    fun Double.formatAsCurrency(): String =
        currencyFormatter().format(this)

    /**
     * Formats the LocalDate as a date string (e.g., "25 Dec 2023").
     *
     * @return A formatted string representation of the date.
     */
    fun LocalDate.formatDate(): String =
        this.format(dateFormatter())

    /**
     * Formats the LocalDate as a date string (e.g., "01 Jan 2023").
     * @return The formatted date string.
     */
    fun LocalDateTime.formatDate(): String =
        this.format(dateFormatter())

    /**
     * Formats a [LocalDate] into a short date string (e.g., "dd MMM").
     *
     * Example: `LocalDate.of(2023, 10, 27)` would be formatted as "27 Oct".
     *
     * The format pattern is "dd MMM" and uses the default [Locale].
     *
     * @receiver The [LocalDate] to format.
     * @return A [String] representing the formatted short date.
     */
    fun LocalDate.formatShortDate(): String =
        this.format(shortDateFormatter())

    // Create a reusable NumberFormat instance for Indian currency (₹)
    // This is efficient as we don't recreate it on every call.
    private val indianCurrencyFormat: NumberFormat by lazy {
        val locale = Locale("en", "IN")
        NumberFormat.getCurrencyInstance(locale)
    }

    /**
     * Formats a BigDecimal value into a proper currency string.
     *
     * Example: 10500.50 -> "₹10,500.50"
     *
     * @param amount The BigDecimal amount to format.
     * @return A formatted currency string (e.g., "₹10,500.50").
     */
    fun formatCurrency(amount: BigDecimal): String {
        return try {
            indianCurrencyFormat.format(amount)
        } catch (e: IllegalArgumentException) {
            // Fallback in case of any formatting error
            "₹${amount.toPlainString()}"
        }
    }
}
