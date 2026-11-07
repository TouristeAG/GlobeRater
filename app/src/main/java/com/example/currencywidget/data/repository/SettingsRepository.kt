package com.example.currencywidget.data.repository

import android.content.Context
import com.example.currencywidget.data.database.SettingsDao
import com.example.currencywidget.data.model.AppSettings
import com.example.currencywidget.widget.WidgetRefresher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDao: SettingsDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    
    fun getSettings(): Flow<AppSettings?> = settingsDao.getSettings()
    
    suspend fun updateSettings(settings: AppSettings) {
        settingsDao.updateSettings(settings)
        WidgetSettingsCache.getInstance(context).updateCache(settings)
        // Widgets only read the cache, so re-render them right away.
        WidgetRefresher.updateAll(context)
    }
    
    suspend fun insertSettings(settings: AppSettings) {
        settingsDao.insertSettings(settings)
        WidgetSettingsCache.getInstance(context).updateCache(settings)
        WidgetRefresher.updateAll(context)
    }
    
    suspend fun getSettingsOrDefault(): AppSettings {
        val settings = settingsDao.getSettings().first()
        return if (settings != null) {
            settings
        } else {
            // Initialize with default settings if none exist
            val defaultSettings = AppSettings()
            insertSettings(defaultSettings)
            defaultSettings
        }
    }
    
    suspend fun resetAllData() {
        settingsDao.deleteAllSettings()
        listOf("ExchangeRatesCache", "SharedBudgetManager", "ConverterWidgetInput", "WeatherStore")
            .forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
        insertSettings(AppSettings())
    }
}