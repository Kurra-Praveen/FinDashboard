package com.kpr.fintrack

import org.junit.Test
import org.junit.Assert.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class TransactionPatternUnitTest {

    @Test
    fun iciciDebitedPattern_matches_expectedGroups() {
        val input = "Rs 11.00 debited from ICICI Bank Savings Account XX750 on 18-Oct-25 towards Google for GOOGLE AutoPay Retrieval Ref No.897984852915"

        val patternStr = "(Rs\\.?|INR)\\s?([\\d,]+\\.\\d{2})\\s+debited from\\s+(.+?)\\s+(Savings|Current)?\\s*Account\\s+(\\w+)\\s+on\\s+(\\d{2}-\\w{3}-\\d{2})\\s+towards\\s+((\\w+)\\s+(\\w+)\\s+(.+?))\\s+Ref No\\.(\\d+)"

        val pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
         val matcher12: Matcher = pattern.matcher(input)

        assertTrue("Pattern should match the input", matcher12.matches())

        // Map expected groups from the user's example
        assertEquals("Rs", matcher12.group(1))
        assertEquals("11.00", matcher12.group(2))
        assertEquals("ICICI Bank", matcher12.group(3))
        assertEquals("Savings", matcher12.group(4))
        assertEquals("XX750", matcher12.group(5))
        assertEquals("18-Oct-25", matcher12.group(6))
        assertEquals("Google for GOOGLE AutoPay Retrieval", matcher12.group(7))
        assertEquals("Google", matcher12.group(8))
        assertEquals("for", matcher12.group(9))
        assertEquals("GOOGLE AutoPay Retrieval", matcher12.group(10))
        assertEquals("897984852915", matcher12.group(11))

        // Optionally print groups for easier debugging in CI logs
        for (i in 0..matcher12.groupCount()) {
            println("group $i = [${matcher12.group(i)}]")
        }
    }
}

