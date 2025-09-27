package com.kpr.fintrack

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kpr.fintrack.domain.model.UpiApp
import com.kpr.fintrack.utils.parsing.ParsingPatternRepository
import com.kpr.fintrack.utils.parsing.TransactionParser
import com.kpr.fintrack.utils.parsing.TransactionParser.ParseResult

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.log

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    suspend fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.kpr.fintrack", appContext.packageName)
    }
}