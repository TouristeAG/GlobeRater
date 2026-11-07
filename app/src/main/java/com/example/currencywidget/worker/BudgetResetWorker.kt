package com.example.currencywidget.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.currencywidget.data.repository.SharedBudgetManager
import com.example.currencywidget.data.repository.WidgetSettingsCache
import com.example.currencywidget.service.WorkScheduler
import com.example.currencywidget.widget.WidgetRefresher

/** Fires at the start of a budget period, resets the spent amount and schedules the next run. */
class BudgetResetWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val budget = SharedBudgetManager.getInstance(applicationContext)
        budget.initializeFromSettingsCache()
        budget.checkAndResetBudgetIfNeeded()
        WidgetRefresher.updateAll(applicationContext)

        val cache = WidgetSettingsCache.getInstance(applicationContext)
        WorkScheduler.scheduleBudgetReset(
            applicationContext,
            budget.getBudgetRefreshStartDate(),
            budget.getBudgetRefreshHour(),
            cache.getBudgetDisplayTimeSpan()
        )
        return Result.success()
    }
}
