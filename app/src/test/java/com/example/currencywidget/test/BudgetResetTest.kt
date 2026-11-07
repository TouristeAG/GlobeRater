package com.example.currencywidget.test

import com.example.currencywidget.data.model.BudgetTimeSpan
import com.example.currencywidget.util.BudgetRefreshCalculator
import org.junit.Test
import org.junit.Assert.*
import java.util.Calendar

class BudgetResetTest {
    
    @Test
    fun testBudgetRefreshCalculator() {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        // Test daily reset - should reset every day at midnight
        val startDate = now - (24 * 60 * 60 * 1000) // Yesterday
        val refreshHour = 0 // Midnight
        
        // Should reset if last reset was before the current period
        val lastResetTime = startDate - (60 * 60 * 1000) // 1 hour before start
        val shouldReset = BudgetRefreshCalculator.shouldRefreshBudget(
            startDate, refreshHour, BudgetTimeSpan.DAILY, lastResetTime
        )
        
        assertTrue("Budget should reset when last reset was before current period", shouldReset)
        
        // Should not reset if already reset in current period
        val recentResetTime = now - (60 * 60 * 1000) // 1 hour ago
        val shouldNotReset = BudgetRefreshCalculator.shouldRefreshBudget(
            startDate, refreshHour, BudgetTimeSpan.DAILY, recentResetTime
        )
        
        assertFalse("Budget should not reset when already reset in current period", shouldNotReset)
    }
    
    @Test
    fun testBudgetTimeSpanCalculation() {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        // Test weekly reset
        val weeklyStartDate = now - (7 * 24 * 60 * 60 * 1000) // 7 days ago
        val weeklyLastReset = weeklyStartDate - (60 * 60 * 1000) // 1 hour before start
        
        val shouldResetWeekly = BudgetRefreshCalculator.shouldRefreshBudget(
            weeklyStartDate, 0, BudgetTimeSpan.WEEKLY, weeklyLastReset
        )
        
        assertTrue("Weekly budget should reset", shouldResetWeekly)
    }
}
