package com.example.currencywidget

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * WorkManager is initialised on demand with the Hilt worker factory.
 *
 * Without this, WorkManager falls back to its default factory, which cannot build
 * @HiltWorker classes (they need injected constructor params), so every background
 * job (exchange rates, weather, budget reset) silently failed to start.
 */
@HiltAndroidApp
class CurrencyWidgetApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.ERROR)
            .build()
}
