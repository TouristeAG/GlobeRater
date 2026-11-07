package com.example.currencywidget.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,
    val inputCurrencies: List<String> = listOf("USD", "EUR", "GBP"),
    val outputCurrencies: List<String> = listOf("USD", "EUR", "GBP", "JPY", "CAD"),
    val budgetCurrency: String = "USD",
    val refreshIntervalDays: Int = 1,
    val budgetTrackingEnabled: Boolean = true,
    val budgetTimeSpan: BudgetTimeSpan = BudgetTimeSpan.MONTHLY,
    val budgetDisplayTimeSpan: BudgetTimeSpan = BudgetTimeSpan.MONTHLY,
    val budgetAmount: Double = 2000.0,
    val budgetRefreshStartDate: Long = System.currentTimeMillis(), // Start date for budget calculation
    val budgetRefreshHour: Int = 0, // Hour of day (0-23) when budget refreshes
    val widgetLayoutType: WidgetLayoutType = WidgetLayoutType.COMPACT,
    val apiKey: String = "",
    val weatherApiKey: String = "",
    val weatherLocation1: String = "",
    val weatherLocation2: String = "",
    val weatherUnits: WeatherUnits = WeatherUnits.METRIC
)

enum class BudgetTimeSpan {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

enum class WidgetLayoutType {
    COMPACT,
    EXPANDED,
    MINIMAL,
    DETAILED,
    HORIZONTAL,
    VERTICAL,
    COMPACT_BUDGET,
    EXPANDED_BUDGET,
    BUDGET_TRACKER,
    VERTICAL_BUDGET_TRACKER,
    HORIZONTAL_BUDGET_TRACKER,
    MINIMAL_BUDGET_TRACKER
}

enum class WeatherUnits {
    METRIC, // Celsius
    IMPERIAL // Fahrenheit
}
