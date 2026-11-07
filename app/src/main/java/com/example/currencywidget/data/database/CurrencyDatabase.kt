package com.example.currencywidget.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.currencywidget.data.model.*

/** Entities stay as they are: changing them would wipe existing settings (destructive migration). */
@Database(
    entities = [
        Currency::class,
        ExchangeRate::class,
        BudgetEntry::class,
        AppSettings::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CurrencyDatabase : RoomDatabase() {
    
    abstract fun currencyDao(): CurrencyDao
    abstract fun settingsDao(): SettingsDao
}
