package com.example.currencywidget.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.currencywidget.data.repository.CurrencyRepository
import com.example.currencywidget.data.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

@HiltWorker
class ExchangeRateUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val currencyRepository: CurrencyRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val apiKey = settingsRepository.getSettingsOrDefault().apiKey.trim()
        // No key: nothing to do. Returning success avoids retry loops that drain battery.
        if (apiKey.isEmpty()) return Result.success()

        val result = currencyRepository.fetchAndUpdateExchangeRates(apiKey)
        return when {
            result.isSuccess -> Result.success()
            // Network hiccup: let WorkManager retry with exponential backoff (few attempts).
            result.exceptionOrNull() is IOException && runAttemptCount < 3 -> Result.retry()
            // Invalid key / quota reached: retrying would not help until the next period.
            else -> Result.success()
        }
    }
}
