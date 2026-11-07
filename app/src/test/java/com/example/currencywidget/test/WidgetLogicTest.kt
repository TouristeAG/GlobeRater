package com.example.currencywidget.test

import com.example.currencywidget.data.model.BudgetTimeSpan
import com.example.currencywidget.util.BudgetRefreshCalculator
import com.example.currencywidget.widget.AmountInput
import com.example.currencywidget.widget.Money
import com.example.currencywidget.widget.RatesCache
import com.example.currencywidget.widget.TravelClockWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class WidgetLogicTest {

    // Number formatting follows the phone's language; these expectations are French.
    @org.junit.Before fun frenchLocale() = java.util.Locale.setDefault(java.util.Locale.FRANCE)

    private fun type(vararg tokens: String) = tokens.fold("") { acc, t -> AmountInput.append(acc, t) }

    @Test fun keypadBuildsNumbers() {
        assertEquals("1250", type("1", "2", "5", "0"))
        assertEquals("12,5", type("1", "2", ",", "5"))
        assertEquals("5000", type("5", "000"))
    }

    @Test fun keypadRejectsInvalidInput() {
        assertEquals("1,2", type("1", ",", ",", "2"))      // single comma
        assertEquals("1,25", type("1", ",", "2", "5", "9")) // two decimals max
        assertEquals("", type("0", "0"))                    // no leading zeros
        assertEquals("0,5", type(",", "5"))                 // comma first → 0,
        assertEquals("1,5", type("1", ",", "000", "5"))     // no 000 after comma
        assertEquals("1234567890", type("1234567890", "1")) // length cap
    }

    @Test fun keypadParsesCommaDecimals() {
        assertEquals(12.5, AmountInput.parse("12,5"), 0.0001)
        assertEquals(0.0, AmountInput.parse(""), 0.0)
        assertEquals("", AmountInput.backspace("5"))
        assertEquals("1 234,5", AmountInput.display("1234,5"))
    }

    @Test fun moneyFormatting() {
        assertEquals("1 234,50 EUR", Money.format(1234.5, "EUR"))
        assertEquals("15 001 JPY", Money.format(15000.6, "JPY"))
        assertEquals("0,00 CHF", Money.format(0.0, "CHF"))
        assertEquals("1 500 000 IDR", Money.format(1_500_000.0, "IDR"))
    }

    @Test fun conversionGoesThroughUsd() {
        val rates = mapOf("USD" to 1.0, "EUR" to 0.9, "CHF" to 0.8)
        assertEquals(90.0, RatesCache.convert(100.0, "USD", "EUR", rates)!!, 1e-9)
        assertEquals(112.5, RatesCache.convert(100.0, "CHF", "EUR", rates)!!, 1e-9)
        assertNull(RatesCache.convert(100.0, "CHF", "THB", rates)) // unknown → no fake 1:1
    }

    @Test fun monthlyBudgetPeriodNeverDriftsIntoTheFuture() {
        // Start on the 28th at midnight, 11 months ago: the current period must start in the past
        // and the next one in the future, whatever the month lengths.
        val start = Calendar.getInstance().apply {
            add(Calendar.MONTH, -11); set(Calendar.DAY_OF_MONTH, 28)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        }.timeInMillis
        val now = System.currentTimeMillis()
        for (span in BudgetTimeSpan.values()) {
            val current = BudgetRefreshCalculator.getCurrentBudgetPeriodStart(start, 0, span)
            val next = BudgetRefreshCalculator.getNextBudgetPeriodStart(start, 0, span)
            assertTrue("$span current <= now", current <= now)
            assertTrue("$span next > now", next > now)
            // Just reset in this period → no new reset.
            assertFalse(BudgetRefreshCalculator.shouldRefreshBudget(start, 0, span, now))
        }
    }

    @Test fun travelClockOffset() {
        val zurich = TimeZone.getTimeZone("Europe/Zurich")
        val bangkok = TimeZone.getTimeZone("Asia/Bangkok")
        val kolkata = TimeZone.getTimeZone("Asia/Kolkata")
        // 2026-01-15 12:00 UTC: Zurich UTC+1 (winter)
        val t = 1768478400000L
        assertEquals(360, TravelClockWidgetProvider.offsetMinutes(zurich, bangkok, t))
        assertEquals(-360, TravelClockWidgetProvider.offsetMinutes(bangkok, zurich, t))
        assertEquals(270, TravelClockWidgetProvider.offsetMinutes(zurich, kolkata, t))
        assertEquals(0, TravelClockWidgetProvider.offsetMinutes(zurich, zurich, t))
        assertEquals("Bangkok", TravelClockWidgetProvider.cityOf("Asia/Bangkok"))
        assertEquals("New York", TravelClockWidgetProvider.cityOf("America/New_York"))
    }

    @Test fun englishSeparators() {
        java.util.Locale.setDefault(java.util.Locale.US)
        assertEquals("1,234.50 EUR", Money.format(1234.5, "EUR"))
        assertEquals("1,234.5", AmountInput.display("1234,5"))
    }
}
