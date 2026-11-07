package com.example.currencywidget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.abs

/** Every widget provider shipped by the app, in one place. */
object WidgetRefresher {

    val providers: List<Class<out android.appwidget.AppWidgetProvider>> = listOf(
        CurrencyConverterWidgetProvider::class.java,
        CurrencyConverterCompactWidgetProvider::class.java,
        CurrencyConverterExpandedBudgetWidgetProvider::class.java,
        CurrencyConverterCompactBudgetWidgetProvider::class.java,
        BudgetTrackerWidgetProvider::class.java,
        HorizontalBudgetTrackerWidgetProvider::class.java,
        MinimalBudgetTrackerWidgetProvider::class.java,
        WeatherTwoDayWidgetProvider::class.java,
        TravelClockWidgetProvider::class.java,
    )

    /** Re-renders every placed widget from the local caches. No network, no wake-ups. */
    fun updateAll(context: Context) {
        providers.forEach { update(context, it) }
    }

    fun updateWeatherWidgets(context: Context) {
        update(context, WeatherTwoDayWidgetProvider::class.java)
        update(context, TravelClockWidgetProvider::class.java)
    }

    fun update(context: Context, provider: Class<out android.appwidget.AppWidgetProvider>) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app) ?: return
        val ids = runCatching { manager.getAppWidgetIds(ComponentName(app, provider)) }.getOrNull()
        if (ids == null || ids.isEmpty()) return
        val intent = Intent(app, provider).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        app.sendBroadcast(intent)
    }

    fun hasAny(context: Context, provider: Class<out android.appwidget.AppWidgetProvider>): Boolean {
        val manager = AppWidgetManager.getInstance(context) ?: return false
        return runCatching {
            manager.getAppWidgetIds(ComponentName(context, provider)).isNotEmpty()
        }.getOrDefault(false)
    }
}

/**
 * PendingIntents are matched on action + data, not extras. Giving each button of each
 * widget instance its own data URI guarantees that two widgets of the same kind never
 * share (and overwrite) each other's click handlers.
 */
fun Intent.uniqueFor(appWidgetId: Int, key: String): Intent = apply {
    data = Uri.parse("globerater://widget/$appWidgetId/${Uri.encode(action ?: "")}/${Uri.encode(key)}")
}

/** Exchange rates cached by [com.example.currencywidget.data.repository.CurrencyRepository] (USD based). */
object RatesCache {
    const val PREFS = "ExchangeRatesCache"
    const val KEY_LAST_UPDATED = "last_updated"

    fun usdRates(context: Context): Map<String, Double> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val rates = HashMap<String, Double>()
        for ((key, value) in prefs.all) {
            if (key.startsWith("rate_") && value is Float && value > 0f) {
                rates[key.substring(5)] = value.toDouble()
            }
        }
        rates["USD"] = 1.0
        return rates
    }

    fun hasRates(context: Context): Boolean = lastUpdated(context) > 0L

    /** Rough rates used only for previews / demo before the user adds an API key. */
    val SAMPLE: Map<String, Double> = mapOf(
        "USD" to 1.0, "EUR" to 0.92, "GBP" to 0.79, "CHF" to 0.88, "JPY" to 150.0, "CAD" to 1.37,
        "AUD" to 1.53, "NZD" to 1.66, "CNY" to 7.2, "HKD" to 7.8, "SGD" to 1.35, "THB" to 35.5,
        "INR" to 83.0, "IDR" to 15800.0, "VND" to 25000.0, "KRW" to 1350.0, "MXN" to 17.0,
        "BRL" to 5.0, "ZAR" to 18.5, "SEK" to 10.6, "NOK" to 10.7, "DKK" to 6.9, "PLN" to 4.0,
        "CZK" to 23.0, "HUF" to 360.0, "TRY" to 32.0, "AED" to 3.67, "MAD" to 10.0, "EGP" to 48.0,
        "PHP" to 56.0, "MYR" to 4.7
    )

    /** Real rates if we have some, otherwise the sample table (flagged). */
    fun ratesOrSample(context: Context): Pair<Map<String, Double>, Boolean> =
        if (hasRates(context)) usdRates(context) to false else SAMPLE to true

    fun lastUpdated(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST_UPDATED, 0L)

    /** Returns null when a rate is unknown, so callers never show a fake 1:1 conversion. */
    fun convert(amount: Double, from: String, to: String, rates: Map<String, Double>): Double? {
        if (from == to) return amount
        val fromRate = rates[from] ?: return null
        val toRate = rates[to] ?: return null
        return amount / fromRate * toRate
    }
}

/** Currency symbols, used in the round badges ("€", "£", "¥"…). */
object CurrencySymbols {
    private val map = mapOf(
        "USD" to "$", "EUR" to "€", "GBP" to "£", "JPY" to "¥", "CNY" to "¥", "CHF" to "Fr",
        "CAD" to "$", "AUD" to "$", "NZD" to "$", "SGD" to "$", "HKD" to "$", "MXN" to "$",
        "SEK" to "kr", "NOK" to "kr", "DKK" to "kr", "ISK" to "kr", "TRY" to "₺", "INR" to "₹",
        "BRL" to "R$", "ZAR" to "R", "KRW" to "₩", "THB" to "฿", "VND" to "₫", "PHP" to "₱",
        "ILS" to "₪", "RUB" to "₽", "UAH" to "₴", "PLN" to "zł", "CZK" to "Kč", "HUF" to "Ft",
        "IDR" to "Rp", "MYR" to "RM", "AED" to "د.إ", "EGP" to "£", "MAD" to "DH", "NGN" to "₦"
    )
    fun of(code: String): String = map[code] ?: code.take(1)
}

object Money {
    private val zeroDecimal = setOf("JPY", "KRW", "VND", "IDR", "CLP", "ISK", "HUF", "PYG", "UGX", "XAF", "XOF")

    /** Separators of the phone's language (1 234,56 in French, 1,234.56 in English…). */
    private fun symbols() = DecimalFormatSymbols.getInstance(java.util.Locale.getDefault())

    fun decimalSeparator(): Char = symbols().decimalSeparator
    fun groupingSeparator(): Char = symbols().groupingSeparator

    fun decimalsFor(currency: String) = if (currency in zeroDecimal) 0 else 2

    fun number(amount: Double, currency: String): String {
        val decimals = decimalsFor(currency)
        // Large values lose their cents so they still fit in a narrow widget line.
        val effective = if (abs(amount) >= 1_000_000) 0 else decimals
        val pattern = if (effective == 0) "#,##0" else "#,##0." + "0".repeat(effective)
        return DecimalFormat(pattern, symbols()).apply { roundingMode = RoundingMode.HALF_UP }.format(amount)
    }

    fun format(amount: Double, currency: String): String = "${number(amount, currency)} $currency"
}

/** Keypad input shared by every converter widget. */
object AmountInput {
    private const val MAX_INTEGER_DIGITS = 10

    fun append(current: String, token: String): String {
        val base = if (current == "0") "" else current
        return when (token) {
            ",", "." -> when {
                base.contains(',') -> base
                base.isEmpty() -> "0,"
                else -> "$base,"
            }
            else -> {
                if (token.any { !it.isDigit() }) return base
                val commaIndex = base.indexOf(',')
                if (commaIndex >= 0) {
                    // Max two decimals; "000" makes no sense after the comma.
                    val decimals = base.length - commaIndex - 1
                    if (token.length > 1 || decimals >= 2) base else base + token
                } else {
                    val next = (base + token).trimStart('0')
                    when {
                        next.isEmpty() -> if (base.isEmpty()) "" else base
                        next.length > MAX_INTEGER_DIGITS -> base
                        else -> next
                    }
                }
            }
        }
    }

    fun backspace(current: String): String = if (current.isEmpty() || current == "0") "" else current.dropLast(1)

    fun parse(text: String): Double = text.replace(',', '.').toDoubleOrNull() ?: 0.0

    /** Typed text keeps ',' as decimal mark; shown with the phone's separators (1 234,5 / 1,234.5). */
    fun display(text: String): String {
        if (text.isEmpty()) return "0"
        val parts = text.split(',', limit = 2)
        val intPart = parts[0].ifEmpty { "0" }
        val grouped = intPart.reversed().chunked(3).joinToString(Money.groupingSeparator().toString()).reversed()
        return if (parts.size == 2) "$grouped${Money.decimalSeparator()}${parts[1]}" else grouped
    }
}

/** "Sky" tile colours for travel widgets, from the local hour at a place. */
object Sky {
    fun isNight(zone: java.util.TimeZone, now: Long = System.currentTimeMillis()): Boolean {
        val hour = java.util.Calendar.getInstance(zone).apply { timeInMillis = now }.get(java.util.Calendar.HOUR_OF_DAY)
        return hour !in 6..19
    }

    /** Tile drawable + text colour resource for a place, in the widget's palette. */
    fun tile(paint: WidgetPaint, zone: java.util.TimeZone): Pair<Int, Int> =
        // Same language as the converter widgets: outlined box by day, solid key at night.
        if (isNight(zone)) paint.d.cNum to paint.r.onAccent else paint.d.cOut to paint.r.accent

    const val ACTION_TICK = "com.example.currencywidget.SKY_TICK"

    /**
     * Hourly, inexact and non-waking alarm so the sky colours follow the day. It only fires
     * when the phone is awake anyway, so it costs next to nothing.
     */
    fun scheduleTicks(context: Context) {
        val am = context.getSystemService(android.app.AlarmManager::class.java) ?: return
        val next = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.HOUR_OF_DAY, 1); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 5)
        }.timeInMillis
        am.setInexactRepeating(android.app.AlarmManager.RTC, next, android.app.AlarmManager.INTERVAL_HOUR, tickIntent(context))
    }

    fun cancelTicksIfUnused(context: Context) {
        if (WidgetRefresher.hasAny(context, TravelClockWidgetProvider::class.java) ||
            WidgetRefresher.hasAny(context, WeatherTwoDayWidgetProvider::class.java)) return
        context.getSystemService(android.app.AlarmManager::class.java)?.cancel(tickIntent(context))
    }

    private fun tickIntent(context: Context) = android.app.PendingIntent.getBroadcast(
        context, 77,
        Intent(context, TravelClockWidgetProvider::class.java).setAction(ACTION_TICK),
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
    )
}
