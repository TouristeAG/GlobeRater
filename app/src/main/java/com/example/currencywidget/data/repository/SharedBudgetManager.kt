package com.example.currencywidget.data.repository

import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager
import com.example.currencywidget.data.model.BudgetTimeSpan
import com.example.currencywidget.util.BudgetRefreshCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized budget manager that handles shared budget data across all widgets
 */
@Singleton
class SharedBudgetManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val PREFS_NAME = "SharedBudgetManager"
        private const val PREF_BUDGET_ENTRIES = "budget_entries"
        private const val PREF_BUDGET_CURRENCY = "budget_currency"
        private const val PREF_BUDGET_AMOUNT = "budget_amount"
        private const val PREF_BUDGET_TIME_SPAN = "budget_time_span"
        private const val PREF_BUDGET_DISPLAY_TIME_SPAN = "budget_display_time_span"
        private const val PREF_BUDGET_REFRESH_START_DATE = "budget_refresh_start_date"
        private const val PREF_BUDGET_REFRESH_HOUR = "budget_refresh_hour"
        private const val PREF_LAST_INPUT_AMOUNT = "last_input_amount"
        private const val PREF_LAST_INPUT_CURRENCY = "last_input_currency"
        
        // Action to notify all widgets of budget changes
        const val ACTION_BUDGET_UPDATED = "com.example.currencywidget.BUDGET_UPDATED"
        
        // For widgets that can't use Hilt injection
        @Volatile
        private var INSTANCE: SharedBudgetManager? = null
        
        fun getInstance(context: Context): SharedBudgetManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SharedBudgetManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val appContext = context.applicationContext
    
    /**
     * Add amount to the shared budget
     */
    fun addToBudget(amount: Double, currency: String) {
        val budgetCurrency = getBudgetCurrency()
        
        // Convert amount to budget currency
        val convertedAmount = convertCurrency(amount, currency, budgetCurrency)
        
        // Get current total spending
        val currentTotal = getTotalSpending()
        val newTotal = currentTotal + convertedAmount
        
        // Store the new total
        prefs.edit().putFloat(PREF_BUDGET_ENTRIES, newTotal.toFloat()).apply()
        
        // Notify all widgets of the budget change
        notifyAllWidgets()
    }

    /**
     * Set the latest input amount and currency coming from any currency widget
     */
    fun setLastInput(amount: Double, currency: String) {
        prefs.edit()
            .putFloat(PREF_LAST_INPUT_AMOUNT, amount.toFloat())
            .putString(PREF_LAST_INPUT_CURRENCY, currency)
            .apply()
    }

    /**
     * Get the latest input amount entered in any currency widget
     */
    fun getLastInputAmount(): Double {
        return prefs.getFloat(PREF_LAST_INPUT_AMOUNT, 0f).toDouble()
    }

    /**
     * Get the latest input currency selected in any currency widget
     */
    fun getLastInputCurrency(): String {
        return prefs.getString(PREF_LAST_INPUT_CURRENCY, getBudgetCurrency()) ?: getBudgetCurrency()
    }
    
    /**
     * Get total spending from shared budget
     */
    fun getTotalSpending(): Double {
        return prefs.getFloat(PREF_BUDGET_ENTRIES, 0f).toDouble()
    }
    
    /**
     * Reset budget to zero
     */
    fun resetBudget() {
        prefs.edit().putFloat(PREF_BUDGET_ENTRIES, 0f).apply()
        notifyAllWidgets()
    }
    
    /**
     * Get budget currency
     */
    fun getBudgetCurrency(): String {
        return prefs.getString(PREF_BUDGET_CURRENCY, "USD") ?: "USD"
    }
    
    /**
     * Set budget currency
     */
    fun setBudgetCurrency(currency: String) {
        prefs.edit().putString(PREF_BUDGET_CURRENCY, currency).apply()
        // Budget-related setting changed; notify widgets to refresh
        notifyAllWidgets()
    }
    
    /**
     * Get budget amount
     */
    fun getBudgetAmount(): Double {
        return prefs.getFloat(PREF_BUDGET_AMOUNT, 2000f).toDouble()
    }
    
    /**
     * Set budget amount
     */
    fun setBudgetAmount(amount: Double) {
        prefs.edit().putFloat(PREF_BUDGET_AMOUNT, amount.toFloat()).apply()
        // Budget-related setting changed; notify widgets to refresh
        notifyAllWidgets()
    }
    
    /**
     * Get budget time span
     */
    fun getBudgetTimeSpan(): String {
        return prefs.getString(PREF_BUDGET_TIME_SPAN, "MONTHLY") ?: "MONTHLY"
    }
    
    /**
     * Set budget time span
     */
    fun setBudgetTimeSpan(timeSpan: String) {
        prefs.edit().putString(PREF_BUDGET_TIME_SPAN, timeSpan).apply()
        // Budget-related setting changed; notify widgets to refresh
        notifyAllWidgets()
    }
    
    /**
     * Get budget display time span
     */
    fun getBudgetDisplayTimeSpan(): BudgetTimeSpan {
        val timeSpanName = prefs.getString(PREF_BUDGET_DISPLAY_TIME_SPAN, BudgetTimeSpan.MONTHLY.name) ?: BudgetTimeSpan.MONTHLY.name
        return try {
            BudgetTimeSpan.valueOf(timeSpanName)
        } catch (e: IllegalArgumentException) {
            BudgetTimeSpan.MONTHLY
        }
    }
    
    /**
     * Set budget display time span
     */
    fun setBudgetDisplayTimeSpan(timeSpan: BudgetTimeSpan) {
        prefs.edit().putString(PREF_BUDGET_DISPLAY_TIME_SPAN, timeSpan.name).apply()
        // Budget-related setting changed; notify widgets to refresh
        notifyAllWidgets()
    }
    
    /**
     * Get scaled budget amount based on display period
     * Always gets the latest settings from WidgetSettingsCache to ensure accuracy
     */
    fun getScaledBudgetAmount(): Double {
        // Always get the latest settings from WidgetSettingsCache
        val settingsCache = WidgetSettingsCache.getInstance(appContext)
        val baseBudgetAmount = settingsCache.getBudgetAmount()
        val budgetTimeSpan = settingsCache.getBudgetTimeSpan().name
        val displayTimeSpan = settingsCache.getBudgetDisplayTimeSpan()
        
        // Calculate scaling factor based on time spans
        val scalingFactor = getTimeSpanScalingFactor(budgetTimeSpan, displayTimeSpan)
        val scaledAmount = baseBudgetAmount * scalingFactor
        
        
        return scaledAmount
    }
    
    /**
     * Budget for the display period, derived from the budget entered for the calculation
     * period: e.g. 3000/month shown weekly = 3000 × 7/30 = 700.
     */
    private fun getTimeSpanScalingFactor(budgetTimeSpan: String, displayTimeSpan: BudgetTimeSpan): Double {
        // Convert time spans to days for calculation
        val budgetDays = when (budgetTimeSpan) {
            "DAILY" -> 1
            "WEEKLY" -> 7
            "MONTHLY" -> 30
            "YEARLY" -> 365
            else -> 30 // Default to monthly
        }
        
        val displayDays = when (displayTimeSpan) {
            BudgetTimeSpan.DAILY -> 1
            BudgetTimeSpan.WEEKLY -> 7
            BudgetTimeSpan.MONTHLY -> 30
            BudgetTimeSpan.YEARLY -> 365
        }
        
        // Return scaling factor: displayDays / budgetDays
        // This gives us the fraction of the budget period that the display period represents
        return displayDays.toDouble() / budgetDays.toDouble()
    }
    fun getBudgetRefreshStartDate(): Long {
        return prefs.getLong(PREF_BUDGET_REFRESH_START_DATE, System.currentTimeMillis())
    }
    
    /**
     * Set budget refresh start date
     */
    fun setBudgetRefreshStartDate(startDate: Long) {
        prefs.edit().putLong(PREF_BUDGET_REFRESH_START_DATE, startDate).apply()
        // Budget-related setting changed; notify widgets to refresh
        notifyAllWidgets()
    }
    
    /**
     * Get budget refresh hour
     */
    fun getBudgetRefreshHour(): Int {
        return prefs.getInt(PREF_BUDGET_REFRESH_HOUR, 0)
    }
    
    /**
     * Set budget refresh hour
     */
    fun setBudgetRefreshHour(hour: Int) {
        prefs.edit().putInt(PREF_BUDGET_REFRESH_HOUR, hour).apply()
        // Budget-related setting changed; notify widgets to refresh
        notifyAllWidgets()
    }
    
    /**
     * Check if budget should be refreshed and reset if needed
     * Uses the display period for reset timing, not the calculation period
     */
    fun checkAndResetBudgetIfNeeded() {
        val refreshStartDate = getBudgetRefreshStartDate()
        val refreshHour = getBudgetRefreshHour()
        val displayTimeSpan = getBudgetDisplayTimeSpan()
        
        // Use BudgetRefreshCalculator to determine if budget should reset
        val shouldReset = BudgetRefreshCalculator.shouldRefreshBudget(
            refreshStartDate, refreshHour, displayTimeSpan, 
            prefs.getLong("last_budget_reset", refreshStartDate)
        )
        
        if (shouldReset) {
            resetBudget()
            prefs.edit().putLong("last_budget_reset", System.currentTimeMillis()).apply()
        }
    }
    
    /**
     * Convert currency using cached exchange rates
     */
    private fun convertCurrency(amount: Double, fromCurrency: String, toCurrency: String): Double {
        val rates = com.example.currencywidget.widget.RatesCache.usdRates(appContext)
        // Unknown rate (no API key yet): keep the amount as typed rather than dropping it.
        return com.example.currencywidget.widget.RatesCache.convert(amount, fromCurrency, toCurrency, rates) ?: amount
    }
    
    /**
     * Notify all widgets of budget changes by sending broadcast
     */
    private fun notifyAllWidgets() {
        val intent = Intent(ACTION_BUDGET_UPDATED)
            .setPackage(appContext.packageName) // ensure delivery to our app's receivers
        appContext.sendBroadcast(intent)        
    }
    
    /**
     * Initialize budget settings from WidgetSettingsCache
     */
    fun initializeFromSettingsCache() {
        val settingsCache = WidgetSettingsCache.getInstance(appContext)
        
        // Only set if not already set
        if (!prefs.contains(PREF_BUDGET_CURRENCY)) {
            setBudgetCurrency(settingsCache.getBudgetCurrency())
        }
        if (!prefs.contains(PREF_BUDGET_AMOUNT)) {
            setBudgetAmount(settingsCache.getBudgetAmount())
        }
        if (!prefs.contains(PREF_BUDGET_TIME_SPAN)) {
            setBudgetTimeSpan(settingsCache.getBudgetTimeSpan().name)
        }
        if (!prefs.contains(PREF_BUDGET_DISPLAY_TIME_SPAN)) {
            setBudgetDisplayTimeSpan(settingsCache.getBudgetDisplayTimeSpan())
        }
        if (!prefs.contains(PREF_BUDGET_REFRESH_START_DATE)) {
            setBudgetRefreshStartDate(settingsCache.getBudgetRefreshStartDate())
        }
        if (!prefs.contains(PREF_BUDGET_REFRESH_HOUR)) {
            setBudgetRefreshHour(settingsCache.getBudgetRefreshHour())
        }
    }
}
