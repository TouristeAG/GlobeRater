package com.example.currencywidget.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.currencywidget.data.model.BudgetTimeSpan
import com.example.currencywidget.util.BudgetRefreshCalculator
import com.example.currencywidget.widget.RatesCache
import com.example.currencywidget.worker.BudgetResetWorker
import com.example.currencywidget.worker.ExchangeRateUpdateWorker
import com.example.currencywidget.worker.WeatherUpdateWorker
import java.util.concurrent.TimeUnit

/**
 * All background work goes through WorkManager so the OS can batch it with other apps
 * (Doze-friendly). Nothing here keeps a service alive or wakes the device on its own.
 */
object WorkScheduler {

    private const val RATES_PERIODIC = "exchange_rate_update"
    private const val RATES_NOW = "exchange_rate_update_now"
    private const val WEATHER_PERIODIC = "weather_update"
    private const val WEATHER_NOW = "weather_update_now"
    private const val BUDGET_RESET = "budget_reset"

    private const val WEATHER_INTERVAL_HOURS = 3L

    private val network = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    // ---- Exchange rates -------------------------------------------------------------

    fun scheduleExchangeRateUpdate(context: Context, refreshIntervalDays: Int) {
        val days = refreshIntervalDays.coerceAtLeast(1).toLong()
        val request = PeriodicWorkRequestBuilder<ExchangeRateUpdateWorker>(days, TimeUnit.DAYS)
            .setConstraints(network)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            RATES_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request
        )
    }

    fun cancelExchangeRateUpdate(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(RATES_PERIODIC)
    }

    /** One fetch as soon as there is network. Used for "refresh now" and stale caches. */
    fun refreshExchangeRatesNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<ExchangeRateUpdateWorker>()
            .setConstraints(network)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(RATES_NOW, ExistingWorkPolicy.KEEP, request)
    }

    /**
     * Periodic work may be deferred for hours by Doze / app standby. When the app is opened
     * and the cache is older than the chosen interval, catch up with a single fetch.
     */
    fun refreshExchangeRatesIfStale(context: Context, refreshIntervalDays: Int) {
        val last = RatesCache.lastUpdated(context)
        val maxAge = TimeUnit.DAYS.toMillis(refreshIntervalDays.coerceAtLeast(1).toLong())
        if (System.currentTimeMillis() - last > maxAge) refreshExchangeRatesNow(context)
    }

    // ---- Weather ----------------------------------------------------------------------

    fun scheduleWeatherUpdate(context: Context) {
        val request = PeriodicWorkRequestBuilder<WeatherUpdateWorker>(WEATHER_INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(network)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WEATHER_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request
        )
    }

    fun refreshWeatherNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<WeatherUpdateWorker>()
            .setConstraints(network)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WEATHER_NOW, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelWeatherUpdate(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WEATHER_PERIODIC)
    }

    // ---- Budget period reset ------------------------------------------------------------

    /**
     * Runs exactly once, at the start of the next budget period, so the widgets show the
     * fresh budget without polling. The worker re-schedules the following one.
     */
    fun scheduleBudgetReset(context: Context, startDate: Long, refreshHour: Int, timeSpan: BudgetTimeSpan) {
        val next = BudgetRefreshCalculator.getNextBudgetPeriodStart(startDate, refreshHour, timeSpan)
        val delay = (next - System.currentTimeMillis()).coerceAtLeast(0L) + 5_000L
        val request = OneTimeWorkRequestBuilder<BudgetResetWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(BUDGET_RESET, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelBudgetReset(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(BUDGET_RESET)
    }

    fun cancelAll(context: Context) {
        cancelExchangeRateUpdate(context)
        cancelWeatherUpdate(context)
        cancelBudgetReset(context)
    }
}
