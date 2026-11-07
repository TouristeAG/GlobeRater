package com.example.currencywidget.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.currencywidget.data.model.AppSettings
import com.example.currencywidget.data.model.BudgetTimeSpan
import com.example.currencywidget.data.model.WeatherUnits

class WidgetSettingsCache private constructor(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "WidgetSettingsCache"
        private const val KEY_INPUT_CURRENCIES = "input_currencies"
        private const val KEY_OUTPUT_CURRENCIES = "output_currencies"
        private const val KEY_DEFAULT_CURRENCY = "default_currency"
        private const val KEY_BUDGET_CURRENCY = "budget_currency"
        private const val KEY_BUDGET_AMOUNT = "budget_amount"
        private const val KEY_BUDGET_TIME_SPAN = "budget_time_span"
        private const val KEY_BUDGET_DISPLAY_TIME_SPAN = "budget_display_time_span"
        private const val KEY_BUDGET_REFRESH_START_DATE = "budget_refresh_start_date"
        private const val KEY_BUDGET_REFRESH_HOUR = "budget_refresh_hour"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_WEATHER_LOCATION_1 = "weather_location_1"
        private const val KEY_WEATHER_LOCATION_2 = "weather_location_2"
        private const val KEY_WEATHER_UNITS = "weather_units"
        
        @Volatile
        private var INSTANCE: WidgetSettingsCache? = null
        
        fun getInstance(context: Context): WidgetSettingsCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WidgetSettingsCache(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    fun updateCache(settings: AppSettings) {
        prefs.edit().apply {
            putString(KEY_INPUT_CURRENCIES, settings.inputCurrencies.joinToString(","))
            putString(KEY_OUTPUT_CURRENCIES, settings.outputCurrencies.joinToString(","))
            putString(KEY_DEFAULT_CURRENCY, settings.inputCurrencies.firstOrNull() ?: "USD")
            putString(KEY_BUDGET_CURRENCY, settings.budgetCurrency)
            putFloat(KEY_BUDGET_AMOUNT, settings.budgetAmount.toFloat())
            putString(KEY_BUDGET_TIME_SPAN, settings.budgetTimeSpan.name)
            putString(KEY_BUDGET_DISPLAY_TIME_SPAN, settings.budgetDisplayTimeSpan.name)
            putLong(KEY_BUDGET_REFRESH_START_DATE, settings.budgetRefreshStartDate)
            putInt(KEY_BUDGET_REFRESH_HOUR, settings.budgetRefreshHour)
            putString(KEY_API_KEY, settings.apiKey)
            putString(KEY_WEATHER_LOCATION_1, settings.weatherLocation1)
            putString(KEY_WEATHER_LOCATION_2, settings.weatherLocation2)
            putString(KEY_WEATHER_UNITS, settings.weatherUnits.name)
            apply()
        }
    }
    
    fun getInputCurrencies(): List<String> {
        val currencies = prefs.getString(KEY_INPUT_CURRENCIES, "USD,EUR,GBP") ?: "USD,EUR,GBP"
        return currencies.split(",").filter { it.isNotEmpty() }
    }
    
    fun getOutputCurrencies(): List<String> {
        val currencies = prefs.getString(KEY_OUTPUT_CURRENCIES, "USD,EUR,GBP,JPY,CAD") ?: "USD,EUR,GBP,JPY,CAD"
        return currencies.split(",").filter { it.isNotEmpty() }
    }
    
    
    
    fun getWeatherLocation1(): String {
        return prefs.getString(KEY_WEATHER_LOCATION_1, "") ?: ""
    }
    
    fun getWeatherLocation2(): String {
        return prefs.getString(KEY_WEATHER_LOCATION_2, "") ?: ""
    }
    
    fun getWeatherUnits(): WeatherUnits {
        val name = prefs.getString(KEY_WEATHER_UNITS, WeatherUnits.METRIC.name) ?: WeatherUnits.METRIC.name
        return runCatching { WeatherUnits.valueOf(name) }.getOrDefault(WeatherUnits.METRIC)
    }
    
    fun getBudgetCurrency(): String {
        return prefs.getString(KEY_BUDGET_CURRENCY, "USD") ?: "USD"
    }
    
    fun getBudgetAmount(): Double {
        return prefs.getFloat(KEY_BUDGET_AMOUNT, 2000.0f).toDouble()
    }
    
    fun getBudgetTimeSpan(): BudgetTimeSpan {
        val timeSpanName = prefs.getString(KEY_BUDGET_TIME_SPAN, BudgetTimeSpan.MONTHLY.name) ?: BudgetTimeSpan.MONTHLY.name
        return try {
            BudgetTimeSpan.valueOf(timeSpanName)
        } catch (e: IllegalArgumentException) {
            BudgetTimeSpan.MONTHLY
        }
    }
    
    fun getBudgetDisplayTimeSpan(): BudgetTimeSpan {
        val timeSpanName = prefs.getString(KEY_BUDGET_DISPLAY_TIME_SPAN, BudgetTimeSpan.MONTHLY.name) ?: BudgetTimeSpan.MONTHLY.name
        return try {
            BudgetTimeSpan.valueOf(timeSpanName)
        } catch (e: IllegalArgumentException) {
            BudgetTimeSpan.MONTHLY
        }
    }
    
    fun getBudgetRefreshStartDate(): Long {
        return prefs.getLong(KEY_BUDGET_REFRESH_START_DATE, System.currentTimeMillis())
    }
    
    fun getBudgetRefreshHour(): Int {
        return prefs.getInt(KEY_BUDGET_REFRESH_HOUR, 0)
    }
}
