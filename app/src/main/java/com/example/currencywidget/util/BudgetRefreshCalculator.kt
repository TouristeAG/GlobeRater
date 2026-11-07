package com.example.currencywidget.util

import com.example.currencywidget.data.model.BudgetTimeSpan
import java.util.Calendar

object BudgetRefreshCalculator {
    
    /**
     * Calculates the current budget period start time based on the configured start date and refresh hour.
     * 
     * @param startDate The configured start date in milliseconds
     * @param refreshHour The hour of day (0-23) when budget refreshes
     * @param timeSpan The budget time span (daily, weekly, monthly, yearly)
     * @return The start time of the current budget period in milliseconds
     */
    fun getCurrentBudgetPeriodStart(
        startDate: Long,
        refreshHour: Int,
        timeSpan: BudgetTimeSpan
    ): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        // Calculate how many periods have passed since the start date
        val periodsPassed = calculatePeriodsPassed(startDate, refreshHour, timeSpan, now)
        
        // Calculate the start of the current period
        return calculatePeriodStart(startDate, refreshHour, timeSpan, periodsPassed)
    }
    
    /**
     * Calculates the next budget period start time.
     */
    fun getNextBudgetPeriodStart(
        startDate: Long,
        refreshHour: Int,
        timeSpan: BudgetTimeSpan
    ): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        // Calculate how many periods have passed since the start date
        val periodsPassed = calculatePeriodsPassed(startDate, refreshHour, timeSpan, now)
        
        // Calculate the start of the next period
        return calculatePeriodStart(startDate, refreshHour, timeSpan, periodsPassed + 1)
    }
    
    /**
     * Index of the period containing [currentTime] (0 = the period starting at the anchor).
     *
     * Periods follow the calendar (real month and year lengths). A first estimate based on
     * the average length is corrected by walking the calendar, so a monthly budget never
     * drifts or lands in the future (which used to wipe the budget on every check).
     */
    private fun calculatePeriodsPassed(
        startDate: Long,
        refreshHour: Int,
        timeSpan: BudgetTimeSpan,
        currentTime: Long
    ): Long {
        val anchor = calculatePeriodStart(startDate, refreshHour, timeSpan, 0)
        val approxLength = when (timeSpan) {
            BudgetTimeSpan.DAILY -> DAY_MS
            BudgetTimeSpan.WEEKLY -> 7 * DAY_MS
            BudgetTimeSpan.MONTHLY -> 30 * DAY_MS
            BudgetTimeSpan.YEARLY -> 365 * DAY_MS
        }
        var n = Math.floorDiv(currentTime - anchor, approxLength)
        while (calculatePeriodStart(startDate, refreshHour, timeSpan, n) > currentTime) n--
        while (calculatePeriodStart(startDate, refreshHour, timeSpan, n + 1) <= currentTime) n++
        return n
    }

    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** Start time of period number [periodNumber] (may be negative). */
    private fun calculatePeriodStart(
        startDate: Long,
        refreshHour: Int,
        timeSpan: BudgetTimeSpan,
        periodNumber: Long
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = startDate
            set(Calendar.HOUR_OF_DAY, refreshHour.coerceIn(0, 23))
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val n = periodNumber.toInt()
        when (timeSpan) {
            BudgetTimeSpan.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, n)
            BudgetTimeSpan.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, n)
            BudgetTimeSpan.MONTHLY -> calendar.add(Calendar.MONTH, n)
            BudgetTimeSpan.YEARLY -> calendar.add(Calendar.YEAR, n)
        }
        return calendar.timeInMillis
    }

    /**
     * Checks if the budget should be refreshed based on the current time and refresh settings.
     */
    fun shouldRefreshBudget(
        startDate: Long,
        refreshHour: Int,
        timeSpan: BudgetTimeSpan,
        lastRefreshTime: Long
    ): Boolean {
        val currentPeriodStart = getCurrentBudgetPeriodStart(startDate, refreshHour, timeSpan)
        
        // Budget should reset if:
        // 1. The last reset was before the current period started, OR
        // 2. The last reset time is 0 (never reset)
        return lastRefreshTime < currentPeriodStart || lastRefreshTime == 0L
    }
}
